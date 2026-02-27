# Firebase Setup Guide for NoteVault

This guide will help you configure Firebase for the NoteVault Android application.

## 📋 Prerequisites

- A Google account
- Access to [Firebase Console](https://console.firebase.google.com/)
- The project's `google-services.json` file (already included)

## 🔥 Step-by-Step Firebase Configuration

### Step 1: Access Firebase Console

1. Go to https://console.firebase.google.com/
2. Login with your Google account
3. Find your NoteVault project (or create a new one if needed)

### Step 2: Enable Email/Password Authentication

This is **CRITICAL** as the app uses email/password authentication, NOT Google Sign-In.

1. In Firebase Console, select your project
2. Click **"Authentication"** in the left sidebar
3. Go to the **"Sign-in method"** tab
4. Find **"Email/Password"** in the providers list
5. Click on it to expand
6. Toggle the **"Enable"** switch to ON
7. Click **"Save"**

**Important:** Do NOT enable "Email link (passwordless sign-in)" - we only need basic email/password.

### Step 3: Verify Firestore Database

1. Click **"Firestore Database"** in the left sidebar
2. If not created yet, click **"Create database"**
3. Choose **"Start in test mode"** (we'll secure it later)
4. Select a location (choose closest to your users)
5. Click **"Enable"**

### Step 4: Set Up Firestore Security Rules

For production use, replace the default rules with secure ones:

1. In Firestore Database, go to the **"Rules"** tab
2. Replace existing rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      // Allow users to read/write their own user document
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Allow users to access their own folders
      match /folders/{folderId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
        
        // Allow users to access notes within their folders
        match /notes/{noteId} {
          allow read, write: if request.auth != null && request.auth.uid == userId;
        }
      }
    }
  }
}
```

3. Click **"Publish"**

These rules ensure:
- Users can only access their own data
- No one can read other users' notes
- Authentication is required for all operations

### Step 5: Verify google-services.json

1. In Firebase Console, click the gear icon ⚙️ → **"Project settings"**
2. Scroll down to **"Your apps"**
3. Find your Android app
4. Download the latest `google-services.json`
5. Replace the file at `/app/app/google-services.json` if needed

**File location:** `/app/app/google-services.json`

### Step 6: Optional - Enable Offline Persistence

This is already enabled in the code, but to verify:

The app automatically enables offline persistence with:
```java
FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .build();
firestore.setFirestoreSettings(settings);
```

This means:
- ✅ App works without internet
- ✅ Data syncs automatically when online
- ✅ Better user experience

## 🧪 Testing Your Firebase Setup

### Test Authentication

1. Build and run the app
2. Go to Registration screen
3. Register with email: `test@example.com`
4. Password: `test123` (or any 6+ char password)
5. Check Firebase Console → Authentication → Users
6. You should see the new user listed

### Test Firestore

1. After registering, create a folder
2. Add a note to the folder
3. Check Firebase Console → Firestore Database
4. Navigate through: `users → [userId] → folders → [folderId] → notes`
5. You should see your data

## 🔒 Security Checklist

Before deploying to production:

- [x] Email/Password authentication enabled
- [x] Firestore security rules configured
- [x] Test mode disabled (rules enforced)
- [ ] Set up Firebase App Check (optional, for advanced security)
- [ ] Enable Firebase Monitoring (optional)
- [ ] Set up Firebase Crashlytics (optional)

## 🆘 Troubleshooting

### Issue: "Sign-in failed" error

**Solution:**
1. Verify Email/Password is enabled in Firebase Console
2. Check that `google-services.json` is up to date
3. Ensure internet connection is active
4. Rebuild the project in Android Studio

### Issue: "Permission denied" in Firestore

**Solution:**
1. Check Firestore security rules are correctly set
2. Verify user is authenticated
3. Ensure userId matches authenticated user's UID

### Issue: Data not syncing across devices

**Solution:**
1. Verify both devices are logged in with same email
2. Check internet connectivity
3. Check Firestore Database for data
4. Try force-closing and reopening the app

### Issue: "Default Firebase App not initialized"

**Solution:**
1. Ensure `google-services.json` is in `/app/app/` directory
2. Sync Gradle files in Android Studio
3. Clean and rebuild project
4. Restart Android Studio if needed

## 📊 Firestore Data Structure

Your database will look like this:

```
notevault-db (Firestore Database)
│
└── users (collection)
    │
    ├── user1-uid (document)
    │   ├── email: "user1@example.com"
    │   ├── uid: "user1-uid"
    │   │
    │   └── folders (sub-collection)
    │       │
    │       ├── folder1-id (document)
    │       │   ├── name: "Personal"
    │       │   ├── color: "#FF6B6B"
    │       │   ├── createdAt: timestamp
    │       │   │
    │       │   └── notes (sub-collection)
    │       │       │
    │       │       ├── note1-id (document)
    │       │       │   ├── title: "My Note"
    │       │       │   ├── content: "Note content..."
    │       │       │   ├── color: "#4ECDC4"
    │       │       │   ├── timestamp: timestamp
    │       │       │   └── updatedAt: timestamp
    │       │       │
    │       │       └── note2-id (document)
    │       │           └── ...
    │       │
    │       └── folder2-id (document)
    │           └── ...
    │
    └── user2-uid (document)
        └── ...
```

## 💡 Best Practices

1. **Regular Backups**
   - Export Firestore data regularly from Firebase Console
   - Keep backups of `google-services.json`

2. **Monitor Usage**
   - Check Firebase Console → Usage and billing
   - Free tier includes: 50K reads, 20K writes, 20K deletes per day

3. **Security**
   - Never share `google-services.json` publicly
   - Keep security rules strict
   - Regularly audit user access

4. **Performance**
   - Firestore automatically indexes common queries
   - Monitor query performance in Firebase Console
   - Offline persistence reduces cloud reads

## 📞 Support

If you encounter issues:

1. Check Firebase Status: https://status.firebase.google.com/
2. Firebase Documentation: https://firebase.google.com/docs
3. Stack Overflow: Tag your question with [firebase] [android]

---

**Firebase Configuration Complete! 🎉**

Your NoteVault app is now ready to use with cloud sync, offline support, and secure authentication.
