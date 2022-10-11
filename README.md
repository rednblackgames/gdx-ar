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
    implementation "androidx.appcompat:appcompat:$appcompatVersion"
}
```

| gdx-ar       | ARCore | AppCompat |
|--------------|--------|-----------|
| 0.1-SNAPSHOT | 1.33.0 |  1.5.1    |

ARCore requires Min SDK 24.

#### ARCore features

List of implemented ARCore features:

- Camera Preview
- Plane Detection
- Hit testing
- Anchor and Pose
- Full Tracking state
- Augmented Images (and Augmented Images Database)
- Light Estimation (AMBIENT_INTENSITY, ENVIRONMENTAL_HDR)
- Camera autofocus

## Basic Implementation

Library implementation in a standard libGDX project is easy. 

### Android

Basic `AndroidLauncher` to enable ARCore capabilities, it's based on `FragmentActivity`:

```Java
/** Launches the Android application. */
public class AndroidLauncher extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
	private static final String TAG = "GDX AR Application";

	private GdxArApplicationListener applicationListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Loads the fragment.  There is no layout for this fragment, so it is simply added.
		ARSupportFragment supportFragment = new ARSupportFragment();

		getSupportFragmentManager().beginTransaction().add(
				supportFragment, ARSupportFragment.TAG).commitAllowingStateLoss();

		applicationListener = new MainApplicationListener(this);

		// Add the listener to check for ARCore being supported and camera permissions are granted.
		supportFragment.getArSupported().thenAccept(useAR -> {
			if (useAR) {
				// Done with the AR support fragment, so remove it.
				removeSupportFragment();

				ARFragmentApplication fragment = new ARFragmentApplication();
				AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
				fragment.setConfiguration(configuration);
				GdxARConfiguration gdxARConfiguration = new GdxARConfiguration();
				fragment.setArApplication(applicationListener, gdxARConfiguration);

				// Finally place it in the layout.
				getSupportFragmentManager().beginTransaction()
						.add(android.R.id.content, fragment)
						.commitAllowingStateLoss();
			}
		}).exceptionally(ex -> {
			Log.e(TAG, "Exception checking for ARSupport", ex);
			return null;
		});
	}

	private void removeSupportFragment() {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(ARSupportFragment.TAG);
		if (fragment != null) {
			getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
		}
	}
}
```

### Core

Basic implementation of the libGDX `ApplicationListener` with AR capabilities:

```Java
public class MainApplicationListener extends GdxArApplicationListener {
    //Augmented Images are really heavy objects to load, async loading is preferred
    private final AsyncExecutor augmentedImagesAsyncExecutor = new AsyncExecutor(2);

    @Override
    public void create(Camera arCamera) {
        //Load everything like AssetManager, init code, etc

        //Example how to load Augmented Images using async thread
        augmentedImagesAsyncExecutor.submit(new AsyncTask<Object>() {
            @Override
            public Object call() throws Exception {
                Array<RawAugmentedImageAsset> images = new Array<>();

                RawAugmentedImageAsset marker = Pools.obtain(RawAugmentedImageAsset.class);
                marker.name = "marker";
                marker.widthInMeter = 1f;
                marker.inputStream = Gdx.files.internal("marker.jpg").read();
                images.add(marker);

                RawAugmentedImageAsset cross = Pools.obtain(RawAugmentedImageAsset.class);
                cross.name = "cross";
                cross.widthInMeter = 0.4f;
                cross.inputStream = Gdx.files.internal("cross.jpg").read();
                images.add(cross);

                getArAPI().buildAugmentedImageDatabase(images);

                Pools.freeAll(images);
                return null;
            }
        });
    }

    @Override
    public void renderARModels(GdxFrame frame) {
        //If AR rendering is enabled this function will be called.
        //Here should be rendered only the 3D AR scene based on GdxFrame information
        //Camera preview rendering is already done by the gdx-ar library
    }

    @Override
    public void render() {
        //Standard libGDX render method that is called after renderARModels
        //Load AssetManager, Draw GUI, Stages or any other things
    }

    @Override
    public void lookingSurfaces(boolean hasSurfaces) {
        //Callback function to notify application that at least a surface has detected by AR framework
    }
    
    //Other standard methods of libGDX ApplicationListener are also supported
    //resize, pause, resume, dispose
}
```

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
