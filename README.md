# RadioPlayerService
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RadioPlayerService-green.svg?style=flat)](http://android-arsenal.com/details/1/2168)

Android service library which uses AAC Player. Ready to use Radio Player Service. (Android Background Player Service)

## Features ##
- Play and stop live radio streams on background.
- Handle incoming and outgoing calls.

## Supported URLs

- http://xxxx:1232
- http://xxxx/abc.pls
- http://xxxx/abc.ram
- http://xxxx/abc.wax
- http://xxxx/abc.m4a
- http://xxxx/abc.mp3

## Unsupported URLs (yet)

NOTE : Some of below urls might have embedded player inside webpage. We can inspect page and check if it has any url
which is supported by this library. I will try to do that next commits.

- rtmp://xxxx
- http://xxxx/abc.aspx
- http://xxxx/abc.php
- http://xxxx/abc.html
- mms://xxxx


# Usage #

## Gradle ##
```
repositories {
    maven {
        url "https://jitpack.io"
    }
}
```

```
dependencies {
    compile 'com.github.iammert:RadioPlayerService:efe3b5420b'
}
```

## Using Radio Player Service ##

In your Activity

```java
RadioManager mRadioManager = RadioManager.with(this);
```
```java
//Invoke it #onCreate
mRadioManager.registerListener(this);
```
```java
//Enables notification or you can disable it 
//giving "false" parameter
mRadioManager.enableNotification(true);
```
```java
//Invoke it #onStart
mRadioManager.connect();
```
```java
//Invoke it #onDestroy
mRadioManager.disconnect();
```

Play and pause radio like 
```java
//starts radio streaming.
mRadioManager.startRadio(RADIO_URL);
//stop radio streaming.
mRadioManager.stopRadio();
```
Implement `RadioListener` to get notified on radio state changed.
```java
public class MainActivity extends Activity implements RadioListener
...
 @Override
    public void onRadioStarted() {
        
    }

    @Override
    public void onRadioStopped() {
        
    }

    @Override
    public void onMetaDataReceived(String s, String s1) {
        
    }
...
```
Any fragments can be informed when it is registered.

```java
public class Fragment1 extends Fragment implements RadioListener
...
//invoke this #onCreateView
RadioManager.with(getActivity()).registerListener(this);

//invoke this #onStop()
RadioManager.with(getActivity()).unregisterListener(this);

```


Demo project will help you to understand implementation.

## Error Case Solutions ##
If you get UnsatisfiedLinkError on some android devices, please go through the [link](https://medium.com/mobiwise-blog/unsatisfiedlinkerror-problem-on-some-android-devices-b77f2f83837d#.c1vlmowal) that I explained how to solve it.


## TODO LIST##

* [x] Create Notification on Background Service.
* [ ] Decode and Buffer size setter methods
* [ ] Decode .php, .html, .aspx web page and inspect supported stream urls if exists.

## Libraries Used ##

[AAC Decoder Library](https://github.com/vbartacek/aacdecoder-android)


License
--------


    Copyright 2015 Mert Şimşek.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.




