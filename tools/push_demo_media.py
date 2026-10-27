"""Copy the downloaded demo pack into a connected debug app and Downloads."""
from pathlib import Path
import subprocess
import base64
import hashlib

ROOT = Path(__file__).resolve().parents[1]
ADB = 'D:/AndroidSDK/platform-tools/adb.exe'
DEVICE = 'emulator-5554'

def adb(*args, **kwargs):
    return subprocess.run([ADB, '-s', DEVICE, *args], check=True, capture_output=True, **kwargs)

if __name__ == '__main__':
    files = sorted((ROOT / 'demo-media').glob('demo_*.*'))
    assert len(files) == 13, 'Download all 13 files first.'
    files += sorted((ROOT / 'demo-media').glob('album_*.jpg'))
    adb('shell', 'am', 'force-stop', 'com.example.appanh')
    adb('shell', 'run-as', 'com.example.appanh', 'mkdir', '-p', 'files/media')
    adb('shell', 'mkdir', '-p', '/sdcard/Download/GalleryDemo')
    for path in files:
        content = path.read_bytes()
        command = f"run-as com.example.appanh sh -c 'base64 -d > files/media/{path.name}'"
        adb('shell', '-T', command, input=base64.b64encode(content) + b'\n')
        digest = adb('shell', 'run-as', 'com.example.appanh', 'sha256sum',
                     f'files/media/{path.name}').stdout.decode().split()[0]
        assert digest == hashlib.sha256(content).hexdigest(), f'Transfer failed: {path.name}'
        adb('push', str(path), f'/sdcard/Download/GalleryDemo/{path.name}')
    print(f'Copied {len(files)} media files; every SHA-256 verified.')
