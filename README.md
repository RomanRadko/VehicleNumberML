# VehicleNumberML
This module is aimed for recognizing car license plate numbers. 
As a base [FirebaseML Kit](https://firebase.google.com/docs/ml-kit) api was used.

<p align="center">
<img src ="https://user-images.githubusercontent.com/4493267/77549078-1135d580-6eb8-11ea-908d-5e3ba6476a7f.png" width="300" height="500">
<img src ="https://user-images.githubusercontent.com/4493267/77549105-1b57d400-6eb8-11ea-95eb-84d45f729543.png" width="300" height="500"> 
</p>

**Preconditions:**
Firebase account should be setuped in the base project, with google-services.json placed in the app root. Here is [firebase integration guide](https://firebase.google.com/docs/android/setup). 

**Usage:**
To get lib .aar file built, need to navigate to Gradle panel in Android Studio, choose:
*routegraph* -> *Tasks* -> *build* -> *assemble*

Recognizing flow should be started with `GetImageFragment` which is androidx.fragment.app.Fragment. As an input arguments possible to set condition to draw a white rectangle overlays.

Example:

```kotlin
    private fun startRecognition() {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        val printFragment: Fragment = GetImageFragment.newInstance()
        printFragment.arguments = Bundle().apply {
            putBoolean(GetImageFragment.SHOULD_DRAW_OVERLAY, true)
        }
        transaction.replace(R.id.container, printFragment)
        transaction.commit()
    }
```
