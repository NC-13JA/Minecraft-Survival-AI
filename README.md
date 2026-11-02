# Minecraft Survival AI

مساعد عربي مرافق للعبة Minecraft Bedrock على Android. المشروع في مرحلة Alpha مبكرة.

## الحالة الحالية
- واجهة Android أولية باستخدام Kotlin وJetpack Compose.
- خدمة نافذة عائمة (Overlay) بإذن صريح من المستخدم.
- إعداد أولي لاتصال Groq Chat Completions.
- لا توجد قراءة شاشة أو خرائط Seed مفعلة بعد؛ ستضاف على مراحل.

## المتطلبات
- Android Studio حديث أو بيئة Android Gradle.
- Android SDK 35.
- Android 8.0 (API 26) أو أحدث.
- مفتاح Groq يضيفه المستخدم بنفسه داخل التطبيق للاختبار.

## تشغيل المشروع
افتح المستودع في Android Studio ثم شغّل مهمة `assembleDebug`.
يمكن تنزيل APK التجريبي من تبويب Actions بعد نجاح بناء GitHub Actions.

## الأمان
لا تضع مفاتيح API في ملفات المصدر أو GitHub أو APK. إدخال المفتاح داخل التطبيق في هذه النسخة مخصص للاختبار الشخصي فقط؛ الاتصال المباشر من APK لا يحمي المفتاح من مستخدم يملك الجهاز. قبل النشر العام يجب نقل استدعاءات Groq إلى خادم وسيط يحتفظ بالمفتاح في Secrets.

## الخطة
1. أساس Android والواجهة العربية والنافذة العائمة.
2. إعداد Groq ومحادثة المساعد.
3. قراءة HUD والإحداثيات عبر MediaProjection وOCR بموافقة المستخدم.
4. Seed map متوافق مع Bedrock وإصدار اللعبة.
5. المهام والموارد والمزارع والتختيم.

## Groq
واجهة Chat Completions: `https://api.groq.com/openai/v1/chat/completions`.
راجع [توثيق Groq](https://console.groq.com/docs/api-reference).
