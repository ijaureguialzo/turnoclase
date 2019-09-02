[Source](https://github.com/fastlane/fastlane/issues/14713#issuecomment-504708205)
 
1. Download the frame from https://applypixels.com/resource/ipad-pro-3rd-gen
2. Subtract the white screen shape from the gray device shapes to make the screen area transparent to background.
3. Export one frame in portrait as PNG with name `Apple iPad Pro (12.9-inch) (3rd generation) Space Gray` to `/Users/user/.fastlane/frameit/latest`
4. Add to `/Users/user/.fastlane/frameit/latest/offsets.json`:
 
```
"iPad Pro (12.9-inch) (3rd generation)": {
	"offset": "+96+102",
	"width": 2046
}
```
