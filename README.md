# `ML-Kit demo` Android app

[ML-Kit](https://developers.google.com/ml-kit) is a free-to-use machine learning API for Android and
iOS apps, focusing
on <b>on-device</b> AI solutions.<br/>
In this repo, I have made a demo of all the AI techniques I have performed with ML-Kit on Android.

The ML techniques used in this demo app include but are not limited to:
<ol>
<li><h3>Face mesh detection</h3></li>
This algorithm detects face mesh info on close-range images.
<br/>
<br/>
Example results:
<br/>
<br/>
<img src="media/face_mesh_1.jpg" width="30%" alt="face mesh 1"/>
<li><h3>Text recognition</h3></li>
This algorithm can recognize and extract text from images.
<br/>
<br/>
Some examples: 
<br/>
<br/>
<img src="media/text_recognition-1.jpg" width="30%" alt="before applying text recognition"/>
<img src="media/text_recognition-2.jpg" width="30%" alt="after applying text recognition"/>
<li><h3>Pose detection</h3></li>
Detecting the position of human body in any given picture/video in real-time.
<br/>
<br/>
Example result:
<br/>
<br/>
<img src="media/pose_detection_1.jpg" width="30%" alt="pose detection 1"/>
<li><h3>Selfie segmentation</h3></li>
Separates the background of a picture/video from users within it. Helps to focus on more important objects in the picture/video.
<br/>
<br/>
<img src="media/selfie_segmentation_1.jpg" width="30%" alt="selfie segmentation 1"/>
<img src="media/selfie_segmentation_2.jpg" width="30%" alt="selfie segmentation 2"/>
<br/>
<br/>
<li><h3>Object detection</h3></li>
Localize and tag in realtime one or more objects in the live camera feed.
<br/>
<br/>
Example results of object detection:
<br/>
<br/>
<img src="media/object_detection_1.jpg" width="30%" alt="Object detection 1"/>
<img src="media/object_detection_2.jpg" width="30%" alt="Object detection 2"/>
<img src="media/object_detection_3.jpg" width="30%" alt="Object detection 3"/>
<img src="media/object_detection_4.jpg" width="30%" alt="Object detection 4"/>
<img src="media/object_detection_5.jpg" width="30%" alt="Object detection 5"/>
<li><h3>Barcode scanner</h3></li>
Scanning and processing most kinds of barcodes. Supports various standard 1D and 2D (a.k.a. QR) barcode formats.
<br/>
<br/>
Example result of barcode scanner:
<br/>
<br/>
<img src="media/barcode_scanner_1.jpg" width="30%" alt="Barcode scanner 1"/>
<br/>
<br/>
A video demo of barcode scanner:
<br/>

<details>
<summary><b>Realtime barcode scanner video demo</b></summary>


https://user-images.githubusercontent.com/8706521/231256249-17ea1166-c330-4a24-9889-1bb8b0100fae.mp4
</details>
<li><h3>Image labeling</h3></li>
This algorithm identifies objects, locations, activities, animal species, products and more in a given picture.<br/>
For example, in the picture below, it has managed to label the <b>road</b>, <b>Jeans</b>, <b>Jacket</b>, and <b>Building</b>s in the picture correctly.
<br/>
<br/>
<img src="media/image_labeling_1.jpg" width="30%" alt="face detection 1"/>
<li><h3>Face detection</h3></li>
Detects faces and facial landmarks in a given image/video.
<br/>
<br/>
Example results:
<br/>
<br/>
<img src="media/face_detection_1.jpg" width="30%" alt="face detection 1"/>
<li><h3>Live Camera translator</h3></li>
This amazing demo camera app firstly determines the language of a string of text with just a few words. And then, translates that text between 58 languages; completely on device. 
<br/>
<br/>
A video demo of realtime camera translator:
<br/>

<details>
<summary><b>Live camera translator video demo</b></summary>


https://user-images.githubusercontent.com/8706521/231260485-a44a559e-6e96-4fd3-aae5-b897d7442d5b.mp4
</details>
<li><h3>Digital ink recognition</h3></li>
This part of the app recognizes handwritten text and hand-drawn shapes (such as emojis) on a digital surface, such as a touch screen. 
<br/>
<br/>
Some examples:
<br/>
<br/>
<img src="media/digital_ink_recognition_1.jpg" width="30%" alt="Digital ink recognition 1"/>
<img src="media/digital_ink_recognition_2.jpg" width="30%" alt="Digital ink recognition 2"/>
<br/>
<br/>
A video demo of digital ink recognition:
<br/>

<details>
<summary><b>Digital ink recognition video demo</b></summary>


https://user-images.githubusercontent.com/8706521/231075026-13e46bb5-c3b5-4e77-8fad-ed2a72e66c89.mp4
</details>

</ol>

### Development:

Please star and fork this repo, I will be maintaining it over time and will try to add more ML
related library demos to
it.<br/>
Feel free to open issues and point out the bugs and short-comings.

new architecture of the app is going to be like this:
we will have these 5 main modules in our codebase:
<ol>
<li>feature_ink_recognition</li>
<li>feature_live_translator</li>
<li>feature_still_image</li>
<li>feature_live_preview</li>
<li>shared</li>
</ol>