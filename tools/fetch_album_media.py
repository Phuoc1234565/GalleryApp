from pathlib import Path
import json, urllib.request, hashlib
root=Path(__file__).resolve().parents[1]/'demo-media'
jobs=[('album_family.jpg','https://images.pexels.com/photos/1682497/pexels-photo-1682497.jpeg?auto=compress&cs=tinysrgb&w=960','https://www.pexels.com/photo/family-walking-on-path-1682497/'),('album_food.jpg','https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=960&q=85','https://unsplash.com/photos/bowl-of-vegetable-salads-IGfIGP5ONV0')]
manifest=[]
for name,url,source in jobs:
    p=root/name
    if not p.exists():
        with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'GalleryApp-demo/1.0'}),timeout=60) as response:p.write_bytes(response.read())
    data=p.read_bytes();assert data[:2]==b'\xff\xd8'
    manifest.append({'file':name,'download':url,'source':source,'sha256':hashlib.sha256(data).hexdigest()})
(root/'album-sources.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
print('Album family/food photos ready')
