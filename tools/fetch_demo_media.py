"""Download the requested demo pack, with source attribution and hashes."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import urllib.request

ROOT = Path(__file__).resolve().parents[1] / 'demo-media'
PHOTOS = [10, 15, 28, 29, 54, 58, 1015, 1016, 1025, 1035]

def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'GalleryApp-demo/1.0'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()

def download(job):
    name, url, info = job
    path = ROOT / name
    content = path.read_bytes() if path.exists() else fetch(url)
    if name.endswith('.jpg'):
        assert content[:2] == b'\xff\xd8', f'Invalid JPEG: {name}'
        attribution = json.loads(fetch(info))
        source = {'author': attribution['author'], 'original': attribution['url']}
    else:
        assert b'ftyp' in content[:32], f'Invalid MP4: {name}'
        source = {'source_page': 'https://samplelib.com/sample-mp4.html'}
    path.write_bytes(content)
    return {'file': name, 'download': url, 'bytes': len(content),
            'sha256': hashlib.sha256(content).hexdigest(), **source}

if __name__ == '__main__':
    ROOT.mkdir(exist_ok=True)
    jobs = [(f'demo_photo_{i:02d}.jpg', f'https://picsum.photos/id/{id}/960/720.jpg',
             f'https://picsum.photos/id/{id}/info') for i, id in enumerate(PHOTOS, 1)]
    jobs += [(f'demo_video_{i:02d}.mp4', f'https://samplelib.com/mp4/sample-{seconds}s-360p.mp4', None)
             for i, seconds in enumerate([5, 10, 15], 1)]
    with ThreadPoolExecutor(max_workers=3) as pool:
        manifest = list(pool.map(download, jobs))
    (ROOT / 'sources.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{len(manifest)} files, {sum(x["bytes"] for x in manifest)/1024/1024:.1f} MiB')
