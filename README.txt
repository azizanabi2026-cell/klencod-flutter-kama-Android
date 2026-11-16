خطوات التشغيل (مجاني)
1) ثبّت Android Studio (مجاني) من developer.android.com/studio
2) New Project ثم Empty Views Activity
   Name: TruckLog   Package name: com.example.trucklog   Language: Kotlin   Minimum SDK: API 24
3) استبدل الملفات التالية بملفات هذا المجلد (نفس المسارات):
   app/src/main/AndroidManifest.xml
   app/src/main/java/com/example/trucklog/MainActivity.kt
   app/src/main/res/layout/activity_main.xml
   وأضف الملف: app/src/main/res/xml/file_paths.xml  (أنشئ مجلد xml إن لم يوجد)
4) افتح app/build.gradle.kts وأضف داخل dependencies { ... } هذا السطر:
   implementation("com.google.mlkit:text-recognition:16.0.1")
   ثم اضغط Sync Now.
5) وصّل الجوال (تفعيل USB debugging) واضغط Run ▶
   للحصول على APK: Build > Build Bundle(s) / APK(s) > Build APK(s)

ملاحظات
- قراءة الأرقام تعمل بدون إنترنت. المكتبة تقرأ الأرقام اللاتينية (0-9)؛ الأرقام العربية-الهندية (٠-٩) لا تُقرأ، ويمكنك كتابتها يدويًا في نافذة التأكيد.
- بعد كل صورة تظهر نافذة تأكيد بالرقم المقروء لتصحيحه قبل الحفظ.
- اضغط مطولًا على أي سطر في الجدول لحذفه.
