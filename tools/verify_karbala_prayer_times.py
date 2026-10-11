#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
verify_karbala_prayer_times.py  —  اختبار قابل لإعادة التنفيذ لمواقيت الصلاة الجعفرية في كربلاء
=======================================================================================

الغرض
-----
1) إعادة تنفيذ خوارزمية PrayTimes.org (نفس منطق JafariPrayerCalculator.kt في تطبيق «أنا مسلم»).
2) مقارنة نتائجها، بالثواني، بحساب فلكي مستقل عالي الدقة (مكتبة PyEphem — خوارزميات VSOP87)
   حتى نفصل «دقة الحساب الفلكي» عن «اختيار المعيار الفقهي».
3) مقارنة ثلاثة ملفات معايير (زاوية الفجر / زاوية المغرب) على إحداثيات العتبتين المقدستين:
      LEVA   : الفجر 16°  / المغرب 4°    (الحالي في التطبيق — PrayTimes "Jafari")
      ARC    : الفجر 18°  / المغرب 4.5°  (القيم الافتراضية في nojumi.org التابع لمكتب السيد السيستاني)
      TEHRAN : الفجر 17.7° / المغرب 4.5° (معهد الجيوفيزياء — جامعة طهران)
4) (اختياري) مطابقة مخرجات التطبيق الأصلية (Kotlin) مع النسخة البايثونية: --kotlin kotlin_out.csv
5) (اختياري) تصدير جدول مرجعي سنوي لاستخدامه في اختبار وحدة داخل التطبيق: --export ref.csv

الاستعمال
---------
    pip install ephem
    python3 verify_karbala_prayer_times.py                # يطبع التقارير
    python3 verify_karbala_prayer_times.py --kotlin kotlin_out.csv --export karbala_reference_2026.csv

ملاحظة: هذا السكربت لا يحدد حكماً فقهياً؛ هو يقيس الفروق الرقمية فقط.
"""
import math, csv, sys, argparse, datetime as dt, statistics as st

try:
    import ephem
except ImportError:  # يعمل الجزء الأول دون ephem
    ephem = None

TZ = 3.0  # Asia/Baghdad = UTC+3 طوال العام (لا توقيت صيفي منذ 2007/2008)

PLACES = {
    "APPDEF": (32.6160, 44.0249),    # القيمة الافتراضية الحالية في PrayerTimesRepository.kt
    "HUSAYN": (32.61639, 44.03250),  # العتبة الحسينية (ويكيبيديا: 32°36′59″N 44°01′57″E)
    "ABBAS": (32.6171889, 44.0362167),  # العتبة العباسية (ويكيبيديا)
}
PROFILES = {"LEVA": (16.0, 4.0), "ARC": (18.0, 4.5), "TEHRAN": (17.7, 4.5)}

# ------------------------------------------------------------------ PrayTimes v2.5 (نسخة بايثون)
def _d2r(d): return d * math.pi / 180.0
def _r2d(r): return r * 180.0 / math.pi
def _sin(d): return math.sin(_d2r(d))
def _cos(d): return math.cos(_d2r(d))
def _tan(d): return math.tan(_d2r(d))
def _fix(a, b):
    a = a - b * math.floor(a / b)
    return a + b if a < 0 else a

def julian(y, m, d):
    if m <= 2:
        y -= 1; m += 12
    A = math.floor(y / 100); B = 2 - A + math.floor(A / 4)
    return math.floor(365.25 * (y + 4716)) + math.floor(30.6001 * (m + 1)) + d + B - 1524.5

def sun_position(jd):
    D = jd - 2451545.0
    g = _fix(357.529 + 0.98560028 * D, 360)
    q = _fix(280.459 + 0.98564736 * D, 360)
    L = _fix(q + 1.915 * _sin(g) + 0.020 * _sin(2 * g), 360)
    e = 23.439 - 0.00000036 * D
    RA = _r2d(math.atan2(_cos(e) * _sin(L), _cos(L))) / 15.0
    eqt = q / 15.0 - _fix(RA, 24)
    decl = _r2d(math.asin(_sin(e) * _sin(L)))
    return decl, eqt

def praytimes(y, m, d, lat, lng, tz, fajr_a, maghrib_a, isha_a=14.0, elv=0.0, midnight_next_fajr=False):
    """يعيد القيم بالساعات العشرية (غير مقرّبة): fajr, sunrise, dhuhr, asr, sunset, maghrib, isha, midnight."""
    jd = julian(y, m, d) - lng / 360.0
    def mid_day(t): return _fix(12 - sun_position(jd + t)[1], 24)
    def angle_time(angle, t, ccw=False):
        decl = sun_position(jd + t)[0]
        x = (-_sin(angle) - _sin(decl) * _sin(lat)) / (_cos(decl) * _cos(lat))
        if x < -1 or x > 1: return float("nan")
        return mid_day(t) + (-1 if ccw else 1) * _r2d(math.acos(x)) / 15.0
    rs = 0.833 + 0.0347 * math.sqrt(max(elv, 0.0))
    t0 = {"fajr": 5, "sunrise": 6, "dhuhr": 12, "asr": 13, "sunset": 18, "maghrib": 18, "isha": 18}
    t0 = {k: v / 24.0 for k, v in t0.items()}
    decl_asr = sun_position(jd + t0["asr"])[0]
    asr_angle = -_r2d(math.atan(1.0 / (1 + _tan(abs(lat - decl_asr)))))
    r = {
        "fajr": angle_time(fajr_a, t0["fajr"], True),
        "sunrise": angle_time(rs, t0["sunrise"], True),
        "dhuhr": mid_day(t0["dhuhr"]),
        "asr": angle_time(asr_angle, t0["asr"]),
        "sunset": angle_time(rs, t0["sunset"]),
        "maghrib": angle_time(maghrib_a, t0["maghrib"]),
        "isha": angle_time(isha_a, t0["isha"]),
    }
    adj = tz - lng / 15.0
    r = {k: v + adj for k, v in r.items()}
    # منتصف الليل الجعفري: بين الغروب والفجر
    if midnight_next_fajr:  # كما في PrayTime v3.2 (فجر اليوم التالي)
        jd2 = jd + 1
        decl = sun_position(jd2 + t0["fajr"])[0]
        noon = _fix(12 - sun_position(jd2 + t0["fajr"])[1], 24)
        x = (-_sin(fajr_a) - _sin(decl) * _sin(lat)) / (_cos(decl) * _cos(lat))
        nf = noon - _r2d(math.acos(x)) / 15.0 + adj + 24
        r["midnight"] = (r["sunset"] + nf) / 2.0
    else:               # كما في PrayTimes v2.5 / تطبيق «أنا مسلم»
        r["midnight"] = r["sunset"] + _fix(r["fajr"] - r["sunset"], 24) / 2.0
    return r

def to_min(h):  # تقريب لأقرب دقيقة مثل PrayTimes (إضافة نصف دقيقة ثم بتر)
    return int(math.floor(_fix(h + 0.5 / 60.0, 24) * 60.0))

def hhmm(m): return "%02d:%02d" % (m // 60, m % 60)

# ------------------------------------------------------------------ مرجع فلكي مستقل (PyEphem)
def ephem_events(y, m, d, lat, lng, fajr_a, maghrib_a):
    """أزمنة عبور مركز الشمس للارتفاعات المطلوبة (دون انكسار جوي: pressure=0) بالساعات المحلية UTC+3.
    منتصف الليل = منتصف ما بين غروب اليوم وفجر اليوم التالي الفعلي (كما في PrayTime v3.2 وكما في فتوى المرجعية)."""
    ob = ephem.Observer()
    ob.lat, ob.lon, ob.elevation, ob.pressure, ob.temp = str(lat), str(lng), 0, 0, 15
    day0 = dt.datetime(y, m, d)
    sun = ephem.Sun()
    def h_local(date, base):  # الساعات المحلية منذ منتصف ليل اليوم base
        return (date.datetime() - base).total_seconds() / 3600.0 + TZ
    def start_of(base): return ephem.Date(base - dt.timedelta(hours=TZ))  # منتصف الليل المحلي
    def rise(alt, base):
        ob.date = start_of(base); ob.horizon = str(-alt)
        return h_local(ob.next_rising(sun, use_center=True), base)
    def sett(alt, base):
        ob.date = start_of(base); ob.horizon = str(-alt)
        return h_local(ob.next_setting(sun, use_center=True), base)
    ob.date = start_of(day0); ob.horizon = "0"
    transit = h_local(ob.next_transit(sun), day0)
    out = {"fajr": rise(fajr_a, day0), "sunrise": rise(0.833, day0), "dhuhr": transit,
           "sunset": sett(0.833, day0), "maghrib": sett(maghrib_a, day0), "isha": sett(14.0, day0)}
    next_fajr = rise(fajr_a, day0 + dt.timedelta(days=1)) + 24.0
    out["midnight"] = (out["sunset"] + next_fajr) / 2.0
    return out

# ------------------------------------------------------------------ تقارير
DATES = [(2026, 1, 15), (2026, 3, 21), (2026, 4, 15), (2026, 6, 21), (2026, 8, 4),
         (2026, 9, 23), (2026, 10, 10), (2026, 11, 15), (2026, 12, 21), (2027, 2, 8)]
KEYS = ["fajr", "sunrise", "dhuhr", "asr", "sunset", "maghrib", "isha", "midnight"]

def report_profiles(place="HUSAYN"):
    lat, lng = PLACES[place]
    print(f"\n## 1) الفروق بالدقائق بين المعايير — {place} ({lat}, {lng}) UTC+3 — مقرّبة لأقرب دقيقة")
    print("التاريخ     | الفجر LEVA | الفجر ARC | الفجر TEH | (ARC−LEVA) | المغرب LEVA | المغرب ARC | (ARC−LEVA) | منتصف الليل LEVA | ARC")
    for (y, m, d) in DATES:
        r = {k: praytimes(y, m, d, lat, lng, TZ, *PROFILES[k]) for k in PROFILES}
        mn = {k: {t: to_min(v) for t, v in r[k].items()} for k in r}
        print("%04d-%02d-%02d  | %s      | %s     | %s     | %+4d       | %s       | %s      | %+4d       | %s            | %s" % (
            y, m, d, hhmm(mn["LEVA"]["fajr"]), hhmm(mn["ARC"]["fajr"]), hhmm(mn["TEHRAN"]["fajr"]),
            mn["ARC"]["fajr"] - mn["LEVA"]["fajr"],
            hhmm(mn["LEVA"]["maghrib"]), hhmm(mn["ARC"]["maghrib"]), mn["ARC"]["maghrib"] - mn["LEVA"]["maghrib"],
            hhmm(mn["LEVA"]["midnight"]), hhmm(mn["ARC"]["midnight"])))

def report_year_sweep(place="HUSAYN"):
    lat, lng = PLACES[place]
    diffs_f, diffs_m, gap_sunset = [], [], {4.0: [], 4.5: [], 6.5: []}
    day = dt.date(2026, 1, 1)
    while day.year == 2026:
        a = praytimes(day.year, day.month, day.day, lat, lng, TZ, 16, 4)
        b = praytimes(day.year, day.month, day.day, lat, lng, TZ, 18, 4.5)
        diffs_f.append((a["fajr"] - b["fajr"]) * 60); diffs_m.append((b["maghrib"] - a["maghrib"]) * 60)
        for ang in gap_sunset:
            c = praytimes(day.year, day.month, day.day, lat, lng, TZ, 18, ang)
            gap_sunset[ang].append((c["maghrib"] - c["sunset"]) * 60)
        day += dt.timedelta(days=1)
    print(f"\n## 2) مسح سنة 2026 كاملة — {place}")
    print("فجر(16°) − فجر(18°)  بالدقائق: min=%.1f  mean=%.1f  max=%.1f" % (min(diffs_f), st.mean(diffs_f), max(diffs_f)))
    print("مغرب(4.5°) − مغرب(4°) بالدقائق: min=%.1f  mean=%.1f  max=%.1f" % (min(diffs_m), st.mean(diffs_m), max(diffs_m)))
    for ang, v in gap_sunset.items():
        print("المغرب عند %.1f° يتأخر عن الغروب (0.833°) بالدقائق: min=%.1f  mean=%.1f  max=%.1f" % (ang, min(v), st.mean(v), max(v)))

def report_vs_ephem(place="HUSAYN"):
    if ephem is None:
        print("\n## 3) (تخطّي) ephem غير مثبتة: pip install ephem"); return
    lat, lng = PLACES[place]
    print(f"\n## 3) PrayTimes مقابل PyEphem (مرجع فلكي مستقل) — {place} — الفرق بالثواني (PrayTimes − Ephem)")
    errs = {k: [] for k in KEYS if k != "asr"}
    day = dt.date(2026, 1, 1)
    while day.year == 2026:
        for prof, (fa, ma) in (("LEVA", PROFILES["LEVA"]), ("ARC", PROFILES["ARC"])):
            p = praytimes(day.year, day.month, day.day, lat, lng, TZ, fa, ma)
            e = ephem_events(day.year, day.month, day.day, lat, lng, fa, ma)
            for k in errs:
                if k == "midnight": continue
                errs[k].append((p[k] - e[k]) * 3600)
        day += dt.timedelta(days=3)
    for k, v in errs.items():
        if v: print("%-8s  mean=%+6.1f s   max|err|=%5.1f s   (n=%d)" % (k, st.mean(v), max(abs(x) for x in v), len(v)))

def report_coordinates():
    print("\n## 4) حساسية الإحداثيات — أقصى فرق بالثواني عبر 10 تواريخ (LEVA) مقارنةً بالعتبة الحسينية")
    ref = PLACES["HUSAYN"]
    for name, (lat, lng) in PLACES.items():
        mx = 0.0
        for (y, m, d) in DATES:
            a = praytimes(y, m, d, lat, lng, TZ, 16, 4); b = praytimes(y, m, d, ref[0], ref[1], TZ, 16, 4)
            mx = max(mx, max(abs(a[k] - b[k]) for k in KEYS) * 3600)
        print("%-7s  (%.5f, %.5f)  أقصى فرق = %.1f ثانية" % (name, lat, lng, mx))
    mx = 0
    for (y, m, d) in DATES:
        a = praytimes(y, m, d, ref[0], ref[1], TZ, 16, 4, elv=30); b = praytimes(y, m, d, ref[0], ref[1], TZ, 16, 4)
        mx = max(mx, abs(a["sunrise"] - b["sunrise"]) * 3600, abs(a["sunset"] - b["sunset"]) * 3600)
    print("أثر الارتفاع 30 م على الشروق/الغروب (تقريب PrayTimes): حتى %.0f ثانية — التطبيق يمرّر 0 م (الصحيح للمعايير المنشورة)." % mx)

def report_misc():
    lat, lng = PLACES["HUSAYN"]
    d1 = []; d2 = []
    day = dt.date(2026, 1, 1)
    while day.year == 2026:
        a = praytimes(day.year, day.month, day.day, lat, lng, TZ, 18, 4.5)
        b = praytimes(day.year, day.month, day.day, lat, lng, TZ, 18, 4.5, midnight_next_fajr=True)
        d1.append((b["midnight"] - a["midnight"]) * 3600)
        d2.append(((a["sunrise"] + a["sunset"]) / 2 - a["dhuhr"]) * 3600)
        day += dt.timedelta(days=7)
    print("\n## 5) تفاصيل")
    print("منتصف الليل: (فجر الغد) − (فجر اليوم) بالثواني: min=%.0f max=%.0f  → أقل من دقيقة، كما ذكر تعليق الشيفرة." % (min(d1), max(d1)))
    print("الزوال: منتصف (الشروق والغروب) − العبور الفعلي بالثواني: min=%.0f max=%.0f" % (min(d2), max(d2)))
    # مثال: صفحة urdupoint «Karbala Shia» ذكرت الظهر 11:39 في 5 يوليو 2026
    a = praytimes(2026, 7, 5, lat, lng, TZ, 16, 4)
    print("الظهر الفلكي في كربلاء 2026-07-05 = %s (مصدر تجميعي ذكر 11:39 ← خطأ ≈ %d دقيقة)" % (hhmm(to_min(a["dhuhr"])), to_min(a["dhuhr"]) - (11 * 60 + 39)))

def compare_kotlin(path):
    print(f"\n## 6) مطابقة مخرجات تطبيق «أنا مسلم» (Kotlin) مع نسخة بايثون — الملف {path}")
    prof = {"A_app_16_4": "LEVA", "B_arc_18_45": "ARC", "C_tehran_177_45": "TEHRAN"}
    bad = tot = 0
    with open(path, encoding="utf-8") as f:
        for row in csv.DictReader(f):
            lat, lng = PLACES[row["place"]]; y, m, d = map(int, row["date"].split("-"))
            r = praytimes(y, m, d, lat, lng, TZ, *PROFILES[prof[row["variant"]]])
            for k in KEYS:
                h, mm = map(int, row[k].split(":"))
                tot += 1
                if to_min(r[k]) != h * 60 + mm:
                    bad += 1; print("  اختلاف:", row["variant"], row["place"], row["date"], k, row[k], hhmm(to_min(r[k])))
    print(f"عدد المقارنات={tot}  غير المتطابقة={bad}")

def export_reference(path, place="HUSAYN"):
    if ephem is None:
        print("ephem مطلوبة للتصدير"); return
    lat, lng = PLACES[place]
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["date", "fajr16", "fajr18", "sunrise", "dhuhr", "sunset", "maghrib4", "maghrib45", "midnight_fajr18"])
        day = dt.date(2026, 1, 1)
        while day <= dt.date(2027, 12, 31):
            a = ephem_events(day.year, day.month, day.day, lat, lng, 16, 4)
            b = ephem_events(day.year, day.month, day.day, lat, lng, 18, 4.5)
            w.writerow([day.isoformat(), hhmm(to_min(a["fajr"])), hhmm(to_min(b["fajr"])), hhmm(to_min(b["sunrise"])),
                        hhmm(to_min(b["dhuhr"])), hhmm(to_min(b["sunset"])), hhmm(to_min(a["maghrib"])),
                        hhmm(to_min(b["maghrib"])), hhmm(to_min(b["midnight"]))])
            day += dt.timedelta(days=1)
    print("تم تصدير المرجع:", path)

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--kotlin"); ap.add_argument("--export"); ap.add_argument("--place", default="HUSAYN")
    a = ap.parse_args()
    report_profiles(a.place); report_year_sweep(a.place); report_vs_ephem(a.place)
    report_coordinates(); report_misc()
    if a.kotlin: compare_kotlin(a.kotlin)
    if a.export: export_reference(a.export, a.place)
