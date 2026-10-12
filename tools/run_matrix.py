"""Isolated live-server + network-client + restart tests. Never accesses a real server.
Uses published Paper JARs, loopback, fresh disposable worlds and matching Java runtimes.
"""
import argparse, concurrent.futures, hashlib, json, os, pathlib, queue, re, shutil, socket, subprocess, sys, threading, time
from datetime import datetime, timezone

ROOT=pathlib.Path(__file__).resolve().parent.parent
JAR=ROOT/'target/DragonTravel-Reforged-2.0.0.jar'

def runtime(v):
    if v=='1.17': return 16
    if v.startswith('26.'): return 25
    parts=tuple(map(int,v.split('.')))
    return 8 if parts<(1,17) else 17 if parts<(1,20,5) else 21

def test(version,args):
    folder=ROOT/'test-servers'/version
    if not (folder/'server.jar').exists(): return dict(version=version,status='unavailable')
    for name in ('test-world','test-world_nether','test-world_the_end','plugins/DragonTravelReforged'):
        target=(folder/name).resolve()
        assert target.is_relative_to((ROOT/'test-servers').resolve())
        if target.exists(): shutil.rmtree(target)
    plugins=folder/'plugins';plugins.mkdir(exist_ok=True);shutil.copy2(JAR,plugins/JAR.name)
    with socket.socket() as probe: probe.bind(('127.0.0.1',0));port=probe.getsockname()[1]
    (folder/'eula.txt').write_text('eula=true\n')
    (folder/'server.properties').write_text(f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\nwhite-list=false\nallow-flight=true\nlevel-name=test-world\nlevel-type=flat\nspawn-protection=0\nview-distance=2\nsimulation-distance=2\nmax-players=4\nallow-nether=false\nspawn-npcs=false\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nnetwork-compression-threshold=-1\n')
    (folder/'bukkit.yml').write_text('settings:\n  allow-end: false\n  connection-throttle: 0\nspawn-limits:\n  monsters: 0\n  animals: 0\n  water-animals: 0\n  ambient: 0\n')
    (folder/'spigot.yml').write_text('world-settings:\n  default:\n    verbose: false\n    view-distance: 2\n')
    (folder/'paper.yml').write_text('world-settings:\n  default:\n    keep-spawn-loaded: false\n    keep-spawn-loaded-range: 0\n')
    (folder/'config').mkdir(exist_ok=True)
    (folder/'config/paper-world-defaults.yml').write_text('spawn:\n  keep-spawn-loaded: false\n  keep-spawn-loaded-range: 0\n')
    java=pathlib.Path(args.runtime_root)/f'jdk{runtime(version)}'/'bin/java.exe'
    cmd=[str(java),'-Xms128M','-Xmx768M','-XX:ActiveProcessorCount=2','-Dfile.encoding=UTF-8','-Ddragontravel.reforged.test=true','-jar','server.jar','nogui']
    started=time.monotonic();log=[];checks=player_checks=0;bot=None;bot_file=None;status='timeout';ready=False
    def client(restart=False):
        nonlocal bot,bot_file
        bot_file=open(folder/('client-restart.log' if restart else 'client-first-boot.log'),'w',encoding='utf-8')
        if version in ('26.2','26.3'):
            command=[sys.executable,str(ROOT/'tools/raw_client.py'),str(port),version,str(plugins/'DragonTravelReforged/protocol.json')]
        else: command=['node',str(ROOT/'tools/test_client.cjs'),str(port),version]
        env=os.environ.copy();env['DTR_PROTOCOL_ROOT']=str(pathlib.Path(args.protocol_root).resolve())
        bot=subprocess.Popen(command,cwd=ROOT,stdout=bot_file,stderr=subprocess.STDOUT,env=env)
    def stop_bot():
        if bot and bot.poll() is None: bot.terminate();bot.wait(timeout=10)
        if bot_file: bot_file.close()
    def boot(restart):
        nonlocal status,checks,player_checks,ready
        process=subprocess.Popen(cmd,cwd=folder,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace',bufsize=1)
        lines=queue.Queue()
        def pump():
            for line in process.stdout:log.append(line);lines.put(line)
        thread=threading.Thread(target=pump,daemon=True);thread.start()
        def send(command):process.stdin.write(command+'\n');process.stdin.flush()
        begin=time.monotonic();sent=False;player_sent=False;player_due=None
        try:
            while time.monotonic()-begin<args.timeout:
                if player_due and time.monotonic()>=player_due and not player_sent:
                    player_sent=True;send('dt recoverycheck Hash014Test' if restart else 'dt playercheck Hash014Test')
                try:line=lines.get(timeout=.5)
                except queue.Empty:
                    if process.poll() is not None:status='boot-failed';return False
                    if bot and bot.poll() is not None:status='client-failed';return False
                    continue
                if 'Done (' in line and not sent:
                    sent=True;ready=True;send('dt persistcheck' if restart else 'dt selftest')
                if 'SELFTEST PASS' in line:
                    checks=int(re.search(r'checks=(\d+)',line).group(1))
                    if version in ('26.2','26.3'):send('dt protocolinfo')
                    else:client()
                if 'PROTOCOL INFO READY' in line:client()
                if 'Hash014Test' in line and 'logged in with entity id' in line and not player_sent:
                    player_due=time.monotonic()+2
                if 'PLAYERTEST PASS' in line:
                    player_checks=int(re.search(r'checks=(\d+)',line).group(1));send('dt persistfixture')
                if 'PERSISTENCE FIXTURE READY' in line:
                    if args.crash:send('dt crashfixture Hash014Test')
                    else:return True
                if 'CRASH FLIGHT READY' in line:
                    process.kill();process.wait(timeout=20);return True
                if 'PERSISTENCE PASS' in line:client(True)
                if 'RECOVERY PASS' in line:status='passed';return True
                if any(s in line for s in ('SELFTEST FAIL','PLAYERTEST FAIL','Error occurred while enabling','Exception executing command','Unhandled exception executing command')):status='test-failed';return False
                if 'Command failed' in line:status='test-failed';return False
            status='restart-timeout' if restart else 'timeout';return False
        finally:
            if process.poll() is None:
                try:send('stop');process.wait(timeout=45)
                except (OSError,subprocess.TimeoutExpired):process.kill();process.wait()
            thread.join(timeout=5)
    try:
        first=boot(False);stop_bot();bot=None
        if first:boot(True)
    except Exception as ex:status='harness-error';log.append(repr(ex)+'\n')
    finally:stop_bot()
    (folder/'reforged-test.log').write_text(''.join(log),encoding='utf-8')
    errors=[s.strip() for s in log if any(k in s for k in ('SELFTEST FAIL','PLAYERTEST FAIL','Could not load','Could not pass event','Error occurred while','NoSuchMethodError','UnsupportedClassVersionError','Journey failed','Cannot enable'))]
    if errors and status=='passed':status='test-failed'
    evidence=(folder/'client-first-boot.log').read_text(encoding='utf-8',errors='replace') if (folder/'client-first-boot.log').exists() else ''
    idfile=plugins/'DragonTravelReforged/test-dragon-ids.txt'
    expected=set(map(int,idfile.read_text().split())) if idfile.exists() else set()
    seen=set(map(int,re.findall(r'CLIENT SPAWN (\d+)',evidence)))
    moves=set(map(int,re.findall(r'CLIENT MOVE (\d+)',evidence)))
    mounts=re.findall(r'CLIENT MOUNT .+',evidence)
    if status=='passed' and (not expected or not expected.issubset(seen) or len(mounts)<3 or not expected.intersection(moves)):
        status='network-evidence-failed';errors.append('Dragon spawn/mount/movement packets missing: '+str(sorted(expected-seen)))
    build=json.loads((folder/'download.json').read_text())
    result=dict(version=version,status=status,selfChecks=checks,playerChecks=player_checks,persistence=status=='passed',forcedProcessKill=bool(args.crash and 'CRASH FLIGHT READY' in ''.join(log)),recoveryAtSpawn=bool(re.search(r'RECOVERY PASS.*fallback=true',''.join(log))),networkDragonsSeen=len(expected.intersection(seen)),networkMountPackets=len(mounts),networkMovingDragons=len(expected.intersection(moves)),pluginSha256=hashlib.sha256((plugins/JAR.name).read_bytes()).hexdigest(),java=runtime(version),paperBuild=build['id'],channel=build['channel'],seconds=round(time.monotonic()-started,1),ready=ready,errors=errors[-8:],log=str((folder/'reforged-test.log').relative_to(ROOT)))
    print(json.dumps(result),flush=True);return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('versions',nargs='*');parser.add_argument('--workers',type=int,default=2);parser.add_argument('--timeout',type=int,default=240)
    parser.add_argument('--crash',action='store_true',help='Kill the disposable JVM during a real flight, then verify recovery')
    parser.add_argument('--runtime-root',default=str(ROOT/'.tools'));parser.add_argument('--protocol-root',default=str(ROOT/'.tools/protocol'));args=parser.parse_args()
    versions=args.versions or sorted(p.name for p in (ROOT/'test-servers').iterdir() if (p/'server.jar').exists())
    results=[];(ROOT/'docs/reforged').mkdir(parents=True,exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures=[pool.submit(test,v,args) for v in versions]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result());(ROOT/'docs/reforged/compatibility-results.json').write_text(json.dumps(dict(testedAt=datetime.now(timezone.utc).isoformat(),results=sorted(results,key=lambda r:tuple(map(int,r['version'].split('.'))))),indent=2),encoding='utf-8')
    sys.exit(0 if all(r['status']=='passed' for r in results) else 1)
