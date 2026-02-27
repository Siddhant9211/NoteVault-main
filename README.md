# NoteVault - Android Notes Application

A modern, cloud-synced notes application built with Java and Firebase, featuring folder organization and beautiful color customization.

## 🎯 Features

### ✅ Implemented Features

1. **Email & Password Authentication**
   - User registration with email validation
   - Secure login system
   - Auto-login for returning users
   - NO Google Sign-In (as per requirements)

2. **Cloud Storage with Firebase Firestore**
   - All data stored in Firestore cloud
   - Automatic sync across devices
   - Data persists after app uninstall/reinstall
   - Offline persistence enabled

3. **Folder-Based Organization**
   - Create custom folders for notes
   - Color-coded folders (10 beautiful presets)
   - Organize notes within folders
   - Delete folders (with all contained notes)

4. **Rich Note Management**
   - Create, edit, and delete notes
   - Color customization for each note
   - Auto-save timestamps
   - Real-time synchronization

5. **Modern UI/UX**
   - Material Design 3 components
   - Gradient backgrounds
   - Colorful folder and note cards
   - Smooth animations
   - Responsive RecyclerView layouts
   - Empty state indicators
   - Loading states

## 📁 Firestore Structure

The app uses a hierarchical Firestore structure for proper data organization:

```
users (collection)
 └── {userId} (document)
     ├── email
     ├── uid
     └── folders (sub-collection)
          └── {folderId} (document)
              ├── name
              ├── color
              ├── createdAt
              └── notes (sub-collection)
                   └── {noteId} (document)
                       ├── title
                       ├── content
                       ├── color
                       ├── timestamp
                       └── updatedAt
```

This structure ensures:
- ✅ Data is linked to user account
- ✅ Login on any device shows same data
- ✅ Data survives app uninstall
- ✅ Organized note management

## 🏗️ Architecture

### Project Structure

```
app/src/main/java/com/example/notevault/
├── activities/
│   ├── SplashActivity.java         # Launch screen with auto-login
│   ├── RegisterActivity.java        # User registration
│   ├── LoginActivity.java           # User login
│   ├── FolderActivity.java          # Main screen showing folders
│   ├── MainActivity.java            # Notes within a folder
│   └── AddEditNoteActivity.java     # Create/edit notes
├── adapter/
│   ├── FolderAdapter.java           # RecyclerView adapter for folders
│   └── NoteAdapter.java             # RecyclerView adapter for notes
├── model/
│   ├── Folder.java                  # Folder data model
│   └── Note.java                    # Note data model
├── viewmodel/
│   ├── FolderViewModel.java         # Folder business logic
│   └── NoteViewModel.java           # Note business logic
├── firebase/
│   └── FirebaseManager.java         # Centralized Firebase operations
└── utils/
    └── ColorUtils.java              # Color palette and utilities
```

### Key Components

1. **FirebaseManager** - Singleton class handling:
   - Email/password authentication
   - Folder CRUD operations
   - Note CRUD operations
   - Real-time listeners
   - Offline persistence

2. **ViewModels** - Architecture Components:
   - LiveData for reactive UI updates
   - Lifecycle-aware data management
   - Clean separation of concerns

3. **Adapters** - RecyclerView implementations:
   - Folder grid with color accents
   - Note list with color strips
   - Click and long-press handlers

## 🎨 UI Features

### Color Palette
10 beautiful preset colors:
- Red (#FF6B6B)
- Turquoise (#4ECDC4)
- Blue (#45B7D1)
- Light Salmon (#FFA07A)
- Mint (#98D8C8)
- Yellow (#F7DC6F)
- Purple (#BB8FCE)
- Sky Blue (#85C1E2)
- Peach (#F8B88B)
- Green (#96CEB4)

### Design Elements
- Gradient backgrounds
- Rounded card corners
- Material 3 components
- Color-coded folder cards
- Color strip on notes
- Floating action buttons
- Loading indicators

## 🚀 Setup Instructions

### Prerequisites
- Android Studio (latest version)
- Firebase account
- Minimum Android API 24 (Android 7.0)

### Firebase Configuration

1. **Enable Email/Password Authentication**
   - Go to Firebase Console
   - Select your project
   - Navigate to Authentication → Sign-in method
   - Enable "Email/Password"
   - Save changes

2. **Firestore Database**
   - Already configured with `google-services.json`
   - Firestore rules are automatically set
   - Offline persistence is enabled

3. **Security Rules** (Recommended)
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

### Build & Run

1. Open project in Android Studio
2. Sync Gradle files
3. Connect Android device or start emulator
4. Click Run (▶️)

## 📱 How to Use

1. **First Time**
   - Open app → Register with email & password
   - Create your first folder with a color
   - Add notes to your folder

2. **Create Folders**
   - Tap the + button
   - Enter folder name
   - Choose a color
   - Tap Create

3. **Add Notes**
   - Tap on a folder
   - Tap the + button
   - Write title and content
   - Choose note color
   - Tap Save

4. **Edit/Delete**
   - Tap note to edit
   - Long-press note to delete
   - Long-press folder to delete (with all notes)

5. **Multi-Device Sync**
   - Login with same email on any device
   - All folders and notes appear automatically
   - Changes sync in real-time

## 🔐 Security Features

- Password minimum length: 6 characters
- Email validation
- Secure Firebase Authentication
- User-specific data isolation
- Firestore security rules ready

## 📦 Dependencies

```kotlin
// Firebase
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")

// Android Architecture Components
implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")

// Material Design
implementation("com.google.android.material:material:latest")

// UI Components
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.cardview:cardview:1.0.0")
implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
```

## ✨ What's New

### Major Changes from Previous Version

1. **Removed Google Sign-In** - Complete migration to email/password authentication
2. **Added Folder System** - Hierarchical note organization
3. **Color Customization** - Beautiful color palette for folders and notes
4. **Modern UI** - Material Design 3 with gradients and animations
5. **Improved Structure** - Better Firestore hierarchy for scalability
6. **Offline Support** - Firestore persistence enabled

## 🐛 Troubleshooting

### Common Issues

1. **Login fails after registration**
   - Ensure Email/Password is enabled in Firebase Console
   - Check internet connection

2. **Notes not syncing**
   - Verify Firestore is enabled
   - Check security rules
   - Ensure internet connectivity

3. **Build errors**
   - Sync Gradle files
   - Clean and rebuild project
   - Update to latest Firebase SDK

## 📄 License

This is a college project for T-YBCA Sem V.

## 🙏 Credits

- Firebase for backend infrastructure
- Material Design for UI components
- Android Jetpack for architecture components

---

**Made with ❤️ for NoteVault users**
