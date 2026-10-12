"""Reuse an explicitly provided Paper cache by hardlinking immutable JARs; no worlds/plugins copied."""
import argparse, hashlib, json, os, pathlib, shutil
ROOT=pathlib.Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('cache_root',type=pathlib.Path);parser.add_argument('versions',nargs='*');args=parser.parse_args()
source=args.cache_root.resolve()/'test-servers'
for version in args.versions or sorted(p.name for p in source.iterdir()):
    folder=source/version
    if not (folder/'server.jar').exists():continue
    meta=json.loads((folder/'download.json').read_text())
    expected=meta['downloads']['server:default']['checksums']['sha256']
    assert hashlib.sha256((folder/'server.jar').read_bytes()).hexdigest()==expected
    target=ROOT/'test-servers'/version;target.mkdir(parents=True,exist_ok=True)
    shutil.copy2(folder/'download.json',target/'download.json')
    candidates=[folder/'server.jar']
    for name in ('cache','libraries','versions'):
        if (folder/name).exists():candidates.extend(p for p in (folder/name).rglob('*') if p.is_file())
    for path in candidates:
        dest=target/path.relative_to(folder);dest.parent.mkdir(parents=True,exist_ok=True)
        if not dest.exists():
            try:os.link(path,dest)
            except OSError:shutil.copy2(path,dest)
    print(version,'verified cache ready',flush=True)
