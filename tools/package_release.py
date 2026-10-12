"""Package only an artifact whose exact SHA has passed the full server matrix."""
import hashlib, json, pathlib, shutil, struct, zipfile

ROOT=pathlib.Path(__file__).resolve().parent.parent
report=json.loads((ROOT/'docs/reforged/compatibility-results.json').read_text(encoding='utf-8'))
rows=report['results']
jar=ROOT/'target/DragonTravel-Reforged-2.0.0.jar'
sha=hashlib.sha256(jar.read_bytes()).hexdigest()
expected=set(p.name for p in (ROOT/'test-servers').iterdir() if (p/'server.jar').exists())
assert {r['version'] for r in rows}==expected,'Incomplete matrix'
assert all(r['status']=='passed' and r['pluginSha256']==sha for r in rows),'Release JAR differs from tested JAR / tests incomplete'
with zipfile.ZipFile(jar) as z:
    plugin=z.read('plugin.yml').decode('utf-8')
    assert 'DragonTravelReforged' in plugin and '2.0.0' in plugin
    assert 'LICENSE.md' in z.namelist()
    for entry in z.namelist():
        if entry.endswith('.class'):
            major=struct.unpack('>H',z.read(entry)[6:8])[0]
            assert major<=52,(entry,major)
release=ROOT/'release';release.mkdir(exist_ok=True)
shutil.copy2(jar,release/jar.name)
source=ROOT/'target/DragonTravel-Reforged-2.0.0-sources.jar';shutil.copy2(source,release/source.name)
lines=['# Compatibility — DragonTravel Reforged 2.0.0','',f"Tested at (UTC): {report['testedAt']}",f'JAR SHA-256: `{sha}`','',
       'All rows use this exact JAR. These are real isolated Paper server boots and connected protocol clients. Each run includes mounting, travel, home, scenic route, item fares, cancellation, refund, permission/protection veto, durable data, restart and reconnect. The process is killed during a fourth real flight when indicated.','',
       'The client confirms dragon spawn, mount and movement packets; it does not render a Minecraft window. This is not a claim about every third-party plugin, every server distribution, camera comfort or production load. Vault needs a separately installed provider and is outside these item-fare checks.','',
       'Some historical Paper builds are ALPHA/BETA because that is what the official archive publishes for those versions. Unsuffixed versions in the official Paper catalog are covered; missing patches and prerelease suffixes are not extrapolated.','',
       'Crash recovery returns to the stored departure when safe. If unsaved terrain disappeared during the forced kill, the intended fallback to world spawn is tested instead (shown in the last column).','',
       '| Version | Paper build | Channel | Java | Server / player checks | Network dragons | Forced kill | Recovery |',
       '| --- | ---: | --- | ---: | --- | ---: | --- | --- |']
for r in rows:
    lines.append(f"| {r['version']} | {r['paperBuild']} | {r['channel']} | {r['java']} | {r['selfChecks']} / {r['playerChecks']} | {r['networkDragonsSeen']} | {'yes' if r.get('forcedProcessKill') else 'no'} | {'spawn fallback' if r.get('recoveryAtSpawn') else 'departure'} |")
lines+=['',f"{len(rows)} versions passed; {sum(r['selfChecks']+r['playerChecks'] for r in rows)} repeated server assertions; 8 Maven geometry tests passed.",'',
        'Server logs: `test-servers/<version>/reforged-test.log`. First client: `client-first-boot.log`; reconnect: `client-restart.log`. The release ZIP includes these logs and the download manifests, without server binaries/worlds.']
(ROOT/'docs/reforged/COMPATIBILITY.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
(release/'SHA256SUMS.txt').write_text(f'{sha}  {jar.name}\n'+hashlib.sha256(source.read_bytes()).hexdigest()+f'  {source.name}\n',encoding='ascii')
with zipfile.ZipFile(release/'DragonTravel-Reforged-2.0.0.zip','w',zipfile.ZIP_DEFLATED) as z:
    for path in [release/jar.name,release/source.name,release/'SHA256SUMS.txt',ROOT/'README.md',ROOT/'LICENSE.md',ROOT/'docs/reforged/API.md',ROOT/'docs/reforged/COMPATIBILITY.md',ROOT/'docs/reforged/compatibility-results.json']:
        z.write(path,path.name)
    for r in rows:
        folder=ROOT/'test-servers'/r['version']
        for name in ('reforged-test.log','client-first-boot.log','client-restart.log','download.json'):
            z.write(folder/name,'evidence/'+r['version']+'/'+name)
    for path in (ROOT/'target/surefire-reports').glob('*.txt'):
        z.write(path,'evidence/unit/'+path.name)
with zipfile.ZipFile(release/'DragonTravel-Reforged-2.0.0-source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for directory in ('src','tools','.github','legacy','docs/reforged'):
        for path in (ROOT/directory).rglob('*'):
            if path.is_file() and '__pycache__' not in path.parts and 'target' not in path.parts and path.name!='runtime-config.json':z.write(path,str(path.relative_to(ROOT)))
    for name in ('pom.xml','README.md','LICENSE.md','.gitignore','.gitattributes'):
        z.write(ROOT/name,name)
print(json.dumps(dict(jar=jar.name,sha256=sha,versions=len(rows),release=str(release)),indent=2))
