# A customized version of the Time_Stamper (v2.1.1) plugin for ImageJ/Fiji
This plugin allows you to add timestamps to your image stacks.　　

## New Features
- **Interactive Positioning**: Users can specify where the timestamp is placed by simply clicking on the image.  
- **UI customization**: Font settings (family, style, size) and colors can be customized directly from the dialog.  
- **Custom Time Formatting**: You can define a custom time format using placeholders:
  - Example: `36min20sec` using `${mm}min${ss}sec`
  - Example: `1d14hr36min20sec` using `${d}d${hh}hr${mm}min${ss}sec`
  - Example: `1:14:36:20` using `${d}:${hh}:${mm}:${ss}`
  * *Note: Selecting the correct `Time Unit` (Seconds, Minutes, Hours) is required to parse the data precisely.*
  * *Priority of time formats: `Custom Format` > `'00:00' format` > `Suffix`  If a custom format pattern is specified, it takes the highest priority, overriding the `'00:00' format` checkbox and any defined `Suffix`.*   
- **Non-destructive Processing**: (Recommended) Checking the `Create New Window` box will generate timestamps on a duplicate window, leaving the original stack unmodified.

## Requirements
ImageJ 1.54g or later (Fiji is recommended).  

## License  
GPLv3+ according to the [original](https://imagej.net/plugins/time-stamper) version.  

## Installation  
Place the `Time_Stamper-2.1.1.jar` file in the `plugins` folder of Fiji or ImageJ (replacing the original version if it exists). Alternatively, you can compile the source code yourself.

## Release Note
2026.06.18  beta released.  
2026.06.19 v2.1.1 released: added the function "Create New Window" when timestamps are generated, which keeps the original images.  

<img src="https://github.com/hiro-shikata/Time_Stamper/blob/master/media/UI4.jpg?raw=true" width="600px">
