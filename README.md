# Drunk Detector

Android app utilising machine learning to detect drunkenness levels in a person based on their movement while the phone is in their pocket.

## Equipment
- Android device - running Android 11 or newer due to Android Auto functionality

## Demo and Report
- **[This video](https://www.youtube.com/watch?v=eP60vuYehAI)** shows:
    * Live demonstration
    * App overview
    * Presentation
- **[This report](DrunkDetectorReport.pdf)** outlines:
    * Motivation
    * Data collection
    * Data processing
    * User feedback


## How it works
- Custom Onnx model trained in Python then loaded to the Java side:
    * Reads from accelerometer and gyroscope within the device
    * Averages the last 4 seconds of data
    * Predicts percentage change of drunkenness
- if predicted drunkenness is > 60%:
    * Send user notification of drunkenness
    * Send SMS message to a chooseable emergency contact
    * Sync with Android Auto to alert the user not to drive 
