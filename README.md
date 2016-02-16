# material-time-picker

Reasonably configurable material-time-picker similar to https://dribbble.com/shots/2518892-Time-picker

<img src= "http://i.imgur.com/G4BHzmW.gif" width="300"/>

Sample app is available on v1 branch

1. Create with builder

  For Activity 
  ```
   final MaterialTimePickerBuilder builder = new MaterialTimePickerBuilder()
   .withActivity(MainActivity.this)
   .withTime(System.currentTimeMillis())
  ```
  
  Referenced activity should implement  MaterialTimePicker.Callbacks               
                
  for Fragment  
  ```
   final MaterialTimePickerBuilder builder = new MaterialTimePickerBuilder()
   .withFragment(fragment, requestCode)
   .withTime(System.currentTimeMillis())
   ```

  Result is passed in Intent to Fragment.onActivityResult, extras: MaterialTimePicker.EXTRA_SELECTED_MILLIS (long)
                
2. Styling 
  
  ```
   Builder.withTheme(themeResId)
  ```

   Library contains default style *R.style.DefMaterialDialogStyle*.
   Supported attributes in referenced styles are same as for TextView.textAppearance   

  ```  
   <style name="DefMaterialDialogStyle">
   <item name="captionTextStyle">@style/DefCaptionTextStyle</item> - top header text appearance
   <item name="captionBackgroundColor">@drawable/top_header_bg</item> - top header background
   <item name="headerBackgroundColor">@color/dialog_header</item> - background behind time digits
   <item name="selectionBackgroundColor">@drawable/animated_bg</item> - background of selected time digit
   <item name="dialogBackgroundColor">@drawable/dialog_bg</item> - dialog background
   <item name="timeColorStyle">@color/datetime_letter_color_selected</item> - time colons appearance
   <item name="digitColorStyle">@style/DefDigitStyle</item> - time digit appearance
   <item name="keyboardColorStyle">@style/DefKbStyle</item> - 10 buttons appearance
   <item name="doneColorStyle">@style/DefDoneBtnStyle</item> - ok button appearance
   </style>
   ```
3. Circular reveal
   ```
   Builder.revealFromView(view);
   ```

4. How to get
  ```
    repositories {
       maven {
           url "https://jitpack.io"
       }
    }
    
    dependencies {
        compile 'com.github.mostroverkhov:material-time-picker:${libVersion}'
    }
    ```
