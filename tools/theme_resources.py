"""Apply semantic day/night colors after regenerating the XML screens."""
from pathlib import Path

RES = Path(__file__).resolve().parents[1] / 'app/src/main/res'

def write(path,text):
    target=RES/path
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(text,encoding='utf-8')

def apply_theme_resources():
    write('values/theme_colors.xml','''<resources>
    <color name="button_primary">#2864FF</color>
    <color name="theme_on_primary">#FFFFFF</color>
    <color name="danger">#CB243C</color>
    <color name="viewer_background">#F6F9FF</color>
    <color name="viewer_controls">#FFFFFF</color>
    </resources>''')
    night={'background':'#10151F','surface':'#1C2534','text_primary':'#EDF3FF',
           'text_secondary':'#ABBBD5','text_hint':'#9EAFCA','field_border':'#52617B',
           'primary':'#91B7FF','primary_light':'#253A61','theme_on_primary':'#102C63',
           'danger':'#FF929D','error':'#FF929D','background_light':'#192231',
           'background_card':'#253145','divider':'#35435A','bottom_nav_bg':'#1C2534',
           'bottom_nav_selected':'#91B7FF','bottom_nav_unselected':'#ABBBD5',
           'tab_selected':'#91B7FF','tab_unselected':'#ABBBD5','tab_indicator':'#91B7FF',
           'chip_selected_bg':'#253A61','chip_selected_text':'#91B7FF',
           'chip_unselected_bg':'#1C2534','chip_unselected_text':'#ABBBD5',
           'trash_bg':'#3A2431','trash_text':'#FF929D',
           'viewer_background':'#080D17','viewer_controls':'#121C2B'}
    write('values-night/colors.xml','<resources>'+''.join(f'<color name="{k}">{v}</color>' for k,v in night.items())+'</resources>')
    # Theme.Material3.DayNight owns both modes. A separate hard-coded Dark
    # parent would discard the common palette/theme attributes at night.
    write('values-night/themes.xml','<resources/>')
    write('values/theme_flags.xml','<resources><bool name="light_system_bars">true</bool></resources>')
    write('values-night/theme_flags.xml','<resources><bool name="light_system_bars">false</bool></resources>')
    for path in (RES/'layout').glob('*.xml'):
        text=path.read_text(encoding='utf-8')
        if path.stem in ['activity_photo_detail','activity_edit_photo']:
            for before,after in {'#080D17':'@color/viewer_background','#121C2B':'@color/viewer_controls',
                                 '#FFFFFF':'@color/text_primary','#D9E5F9':'@color/text_secondary',
                                 '#C1CEE0':'@color/text_secondary'}.items():
                text=text.replace(before,after)
        else:
            text=text.replace('#FFFFFF','@color/white').replace('#E8F2FF','@color/primary_light')
        text=text.replace('#DB3546','@color/danger')
        text=text.replace('app:backgroundTint="@color/primary"','app:backgroundTint="@color/button_primary"')
        path.write_text(text,encoding='utf-8')
    for path in (RES/'drawable').glob('bg_*.xml'):
        if path.stem in ['bg_logo_tile','bg_photo_overlay','bg_video_duration','bg_splash_gradient','bg_auth_surface']:
            continue
        text=path.read_text(encoding='utf-8')
        for before,after in {'@color/white':'@color/surface','#FFFFFF':'@color/surface',
                             '#F5F7FA':'@color/background_light','#E5E7EB':'@color/field_border',
                             '#E8F0FE':'@color/primary_light','#4A90D9':'@color/primary'}.items():
            text=text.replace(before,after)
        path.write_text(text,encoding='utf-8')
    for folder in ['drawable','drawable-night']:
        write(f'{folder}/bg_auth_surface.xml','''<shape xmlns:android="http://schemas.android.com/apk/res/android"><gradient android:angle="315" android:startColor="@color/surface" android:centerColor="@color/background" android:endColor="@color/background_card"/></shape>''')
    path=RES/'color/chip_background.xml'
    path.write_text(path.read_text(encoding='utf-8').replace('@color/primary','@color/button_primary'),encoding='utf-8')
    path=RES/'values-v31/themes.xml'
    path.write_text(path.read_text(encoding='utf-8').replace('#F3F7FF','@color/background').replace('>true<','>@bool/light_system_bars<'),encoding='utf-8')

if __name__=='__main__': apply_theme_resources()
