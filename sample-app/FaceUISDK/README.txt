FaceUISDK for Android
=====================

1. Introduction

FaceUISDK is a Android Library designed to provide application makers an easy way to integrate facial authentication features from the Ezio Mobile SDK into their application.

The library provides User Interface (UI) Listeners, Enrollment & Verification Manager classes, as well as UI elements such as Fragments and XMLs layouts. For example usage on how to use FaceUISDK, please refer to “eziomobilesdk_facial_example” project.

2. Prerequisites

* A Linux or Windows development platform
* Android Studio 2.1.2
* Android SDK 6.0 or above with at least one platform target setup.

3. Configure the Library

FaceUISDK has 4 dependencies:

* EzioMobileSDK
* Android v7 Appcompat Library
* Android v4 Support Library
* Java Native Access (JNA)

In order to compile the library, copy the Ezio Mobile SDK library to the project, i.e. the ./FaceUISDK/libs directory. If the libs folder does not exist, create it.
The other dependencies will be resolved by gradle from their respective maven repositories. Ensure that you have a working internet connection to download them.

4. Using the Library

In order to use the library in your application, we will need to include it as a project. Modify ‘settings.gradle’ (if it’s not present, create it) to:

include ‘:yourappnamehere’, ':FaceUISDK'
// Modify the path to the FaceUISDK project below 
project(':FaceUISDK').projectDir = new File(settingsDir, '../eziomobilesdk_facial_uisdk/FaceUISDK')