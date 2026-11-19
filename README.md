# HopTell — Smart AI Voice Translator (Android)

**HopTell** — jonli nutqni bir zumda aniqlab, neyron tarmoqlar (Google Gemini AI) orqali tabiiy rus, turk, ingliz, xitoy va o'zbek tillariga tarjima qiluvchi hamda Android ovoz tizimida ravon gapirib beruvchi **mustaqil mobil ilova**.

---

## 🌟 Ommaga Chiqarish Uchun Asosiy Afzalliklari

1. **Hech Qanday Kompyuter yoki Server Talab Etilmaydi**:
   - Ilova foydalanuvchilar uchun 100% mustaqil (Standalone) ishlaydi.
   - Telefon va kompyuter bitta Wi-Fi da bo'lishi **shart emas**.
   - Foydalanuvchi oddiy mobil internet (4G/5G) yoki istalgan Wi-Fi orqali dunyoning istalgan burchagida bemalol foydalana oladi.

2. **Tezkorlik (Single-Shot Multimodal AI)**:
   - Gapirib tugatishingiz bilan audioni to'g'ridan-to'g'ri Google Gemini AI tahlil qiladi:
     - 1. Nutqni aniqlash (STT);
     - 2. Tabiiy grammatik tarjima (Translation);
   - Hammasi bitta so'rovda ~1 soniyada amalga oshiriladi.

3. **Android Tizim Ovoz Sintezi (TTS)**:
   - Android telefonlardagi Google Text-to-Speech tizimi orqali 0 millisekund kechikish bilan neyron ovozda gapiradi.
   - Ovoz yuklab olinishini kutish shart emas, tarjima chiqishi bilanoq gapiradi.

4. **Apple Intelligence Glassmorphism Dizayn**:
   - WebGL asosida jonlanadigan "Living Orb" animatsiyasi.
   - Tungi/kunduzgi mavzu, suhbatlar tarixi va matn kiritish imkoniyati.
   - Tayyor audio fayllarni yuklab tarjima qilish.

---

## 📱 APK Yaratish Tartibi

### 1-usul: Android Studio orqali (Tavsiya etiladi)
1. **Android Studio** dasturini oching.
2. `Open` tugmasini bosib, quyidagi papkani tanlang:
   ```text
   D:\anti\perevodchik apk\android
   ```
3. Android Studio avtomatik ravishda Gradle loyihasini yuklaydi.
4. Menyudan: `Build` ➔ `Build Bundle(s) / APK(s)` ➔ `Build APK(s)` ni bosing.
5. Tayyor `app-debug.apk` fayli quyidagi manzilda chiqadi:
   `android\app\build\outputs\apk\debug\app-debug.apk`

---

### 2-usul: GitHub Actions orqali (1-klikda onlayn yig'ish)
Loyihada `.github/workflows/build-apk.yml` fayli mavjud:
1. Loyihani GitHub-ga push qiling (`git push origin main`).
2. GitHub sahifangizdagi **Actions** bo'limiga o'ting.
3. 2 daqiqada tayyor bo'lgan `HopTell-Debug-APK.zip` ni yuklab oling!

---

### 3-usul: PowerShell skripti orqali
```powershell
.\build-apk.ps1
```
