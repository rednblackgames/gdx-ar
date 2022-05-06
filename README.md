# gdx-ar

![maven-central](https://img.shields.io/maven-central/v/games.rednblack.gdxar/core?color=blue&label=release)
![sonatype-nexus](https://img.shields.io/nexus/s/games.rednblack.gdxar/core?label=snapshot&server=https%3A%2F%2Foss.sonatype.org)

Augmented Reality extension for libGDX. The library is composed by an abstraction layer for common AR operations and backend specific implementation.

### Supported platforms

- Android : [Google ARCore](https://developers.google.com/ar)


## How to use

Include gdx-ar in `core` project:

```Groovy
dependencies {
	api "games.rednblack.gdxar:core:$gdxARVersion"
}
```

### Android

Include android platform specific dependence:
```Groovy
dependencies {
    implementation "games.rednblack.gdxar:android:$gdxARVersion"
    implementation "com.google.ar:core:$arcoreVersion"
    implementation "com.google.android.material:material:$materialVersion"
    implementation "androidx.appcompat:appcompat:$appcompatVersion"
}
```

| gdx-ar | ARCore | Material | AppCompat |
|--------| ------ |----------|-----------|
| 0.1    | 1.30.0 | 1.6.0    | 1.4.1     |

#### ARCore features

List of implemented ARCore features:

- Camera Preview
- Plane Detection
- Hit testing
- Anchor and Pose
- Full Tracking state
- Augmented Images (and Augmented Images Database)
- Light Estimation (AMBIENT_INTENSITY, ENVIRONMENTAL_HDR)

#### Basic Implementation

Soon :)

## TODO

- ARCore Depth Testing: https://developers.google.com/ar/develop/depth
- iOS Backend using ARKit

### License

ARCore backend implementation is inspired by [helloargdx](https://github.com/google/helloargdx).

`gdx-ar` is available under the Apache 2.0 Open Source License.
```
Copyright (c) 2022 Francesco Marongiu.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```