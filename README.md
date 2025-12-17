# CampusReach 🎓

> Connecting EPFL students for mutual support and campus event discovery

[![Figma Design](https://img.shields.io/badge/Figma-Design-purple?logo=figma)](https://www.figma.com/design/yc3KeqSlTPlHXqwDUrHBgG/CampusReach?m=auto&t=KTpDechYdmQCsms0-6)

---

## About the Project

CampusReach is a mobile application designed to strengthen the EPFL student community by facilitating peer-to-peer support and enhancing campus life discovery. Whether you need to borrow course materials, find someone to study with, or discover spontaneous campus events, CampusReach brings students together in a trusted, campus-exclusive environment.

The app addresses a common challenge faced by university students: the difficulty of finding help or connecting with peers outside of formal channels. By creating a dedicated platform for student collaboration and event sharing, CampusReach fosters a more connected and supportive campus community.

## Core Features

### Handle Requests

**Create Requests:**
- Borrow textbooks, lab equipment, calculators, or other academic materials
- Find study partners, project collaborators, or tutoring help
- Set a **time and date** for the request
- Set **location** when creating a request
- Organize by academic subject, urgency, and type of assistance needed

**Interact with Requests:**
- Give **kudos** to users who accepted their request
- Visualize **color-coded tags** for requests posted by followers or followees
- **Filter requests** (by subject, urgency, location, etc.)

**Manage Requests:**
- Consult **own requests** and **accepted requests**
- **Delete** requests

### Profile Features

**Personal Profile (Private, Login Required):**
- View and edit **personal details, interests, and activity history**
- Customize profile sections (e.g., skills, courses, hobbies)
- Consult **followers** and **followees**
- Manage **privacy settings** for profile visibility

**Public Profile (Visible to All Users):**
- View **basic user details** (name, profile picture, skills, courses)
- See **public activity history** (e.g., requests accepted, kudos received)
- Follow/unfollow users

### Interactive Campus Map
- Visualize **active support requests** with location markers
- **Real-time updates** for new requests
- **Proximity-based filtering** for nearby opportunities

### Chat Feature
- **Direct messaging** between users for coordination and support
- **Real-time chat** for quick communication

### Trust & Safety System
- **User rating system** based on completed interactions
- **Kudos system** to recognize helpful contributions
- **Report and moderation features** for maintaining community standards
- **Transaction history** for accountability

### Leaderboard
- **Fully customizable leaderboard** to rank users based on activity, kudos, and contributions
- **Sort and filter** by profile components (e.g., kudos, skills, subjects, or activity level)

### Offline Mode
- View **cached support requests** and details
- Browse **previously loaded content**
- Access **saved locations** and favorite requests
- **Automatic sync** when connection is restored

## Design

View our complete UI/UX design, user flows, and interactive prototypes on Figma:

**[→ CampusReach Design File](https://www.figma.com/design/yc3KeqSlTPlHXqwDUrHBgG/CampusReach?m=auto&t=KTpDechYdmQCsms0-6)**

## Technology Stack

- **Platform**: Android (Kotlin)
- **Backend**: Firebase (Authentication, Firestore, Cloud Functions)
- **Maps**: Google Maps SDK for Android
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Framework**: Jetpack Compose

## Getting Started

### Prerequisites
- Android Studio (latest version)
- JDK 11 or higher
- Android SDK (API level 24+)
- Firebase account
