# WebSocket Visualizer with OpenLayers

## Requirements

Only NodeJS (https://nodejs.org/en/download/)

## Install Development Requirements

Change to the visualizer directory:  
`cd <MOSAIC_SOURCE>/fed/mosaic-output/src/main/resources/web`

Install required modules for development and deployment (defined in package.json):  
`npm install`

That's it.

## Development

It is recommended to use VSCode (https://code.visualstudio.com/download) for the development of WebVisualizer since it is free and offers a very good supports for easy JS development like linting and suggestions.
If you want to make changes you need to make them in the `visualizer-dev.js`.  
You can install eslint (https://eslint.org/) globally with `npm i -g eslint` to support you to write correct code.

### Markers / Icons
        
Icons are form [https://mapicons.mapsmarker.com/](https://mapicons.mapsmarker.com/). \
Please use the 3rd icon, which has a slight color gradient in the background.\
Please use following colors: \
grey `6d6d6d` \
black `080808` \
green `23ff23` \
red `ff1c1c`


## Building visualizer.js

To deploy your changes you have three possibilities:  
1. Creating a readable version with `npm run-script build`.
2. Creating a non-readable version with `npm run-script buildUgly`. This version is loaded faster by the browser. Use this command for the final version of your MR, otherwise the compiled files will be blown-up in size.
3. To debug your changes create `visualizer.js` with the command `npm run-script debug`.
This will add debugging information to `visualizer.js` which will be parsed by a modern browser,
such that you backtrack error to individual javascript files using the browser's developer tools.

Given that you develop with the IntelliJ IDE you have to run `mvn validate` in the "Eclipse MOSAIC Starter" so that the files are copied to the `target` folder. 