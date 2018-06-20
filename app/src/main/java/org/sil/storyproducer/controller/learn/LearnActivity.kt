package org.sil.storyproducer.controller.learn

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.res.ResourcesCompat
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView

import org.sil.storyproducer.R
import org.sil.storyproducer.controller.phase.PhaseBaseActivity
import org.sil.storyproducer.model.Story
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.Workspace.activeStory
import org.sil.storyproducer.model.logging.LearnEntry
import org.sil.storyproducer.tools.BitmapScaler
import org.sil.storyproducer.tools.file.*
import org.sil.storyproducer.tools.file.AudioFiles
import org.sil.storyproducer.tools.file.ImageFiles
import org.sil.storyproducer.tools.media.AudioPlayer
import org.sil.storyproducer.tools.media.MediaHelper
import org.sil.storyproducer.tools.toolbar.RecordingToolbar

import java.io.File
import java.util.ArrayList

class LearnActivity : PhaseBaseActivity() {

    private var rootView: RelativeLayout = findViewById(R.id.phase_frame)
    private var learnImageView: ImageView = findViewById(R.id.learnImageView)
    private var playButton: ImageButton = findViewById(R.id.playButton)
    private var videoSeekBar: SeekBar = findViewById(R.id.videoSeekBar)
    private var narrationPlayer: AudioPlayer = AudioPlayer()
    private var backgroundPlayer: AudioPlayer = AudioPlayer()

    private var slideNumber = 0
    private var isVolumeOn = true
    private var isWatchedOnce = false
    private val backgroundAudioJumps: MutableList<Int> = ArrayList()

    //recording toolbar vars
    private val recordFilePath: String? = null
    private var recordingToolbar: RecordingToolbar = RecordingToolbar(this,
            layoutInflater.inflate(R.layout.toolbar_for_recording, rootView, false),
            rootView, true, false, false, false,
            recordFilePath, recordFilePath, null, object : RecordingToolbar.RecordingListener {
        override fun onStoppedRecording() {}//empty because the learn phase doesn't use this
        override fun onStartedRecordingOrPlayback(isRecording: Boolean) {resetVideoWithSoundOff()}
    })

    private var isFirstTime = true         //used to know if it is the first time the activity is started up for playing the vid
    private var startPos = -1
    private var startTime: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn)

        setBackgroundAudioJumps()

        setSeekBarListener()

        setPic(learnImageView)     //set the first image to show

        //set the recording toolbar stuffs
        invalidateOptionsMenu()
        recordingToolbar.keepToolbarVisible()

        //The following allows for a touch from user to close the toolbar and make the fab visible.
        //This does not stop the recording
        val dummyView = rootView.findViewById<RelativeLayout>(R.id.activity_learn)
        dummyView.setOnClickListener {
            if (recordingToolbar.isOpen && !recordingToolbar.isRecording) {
                recordingToolbar.keepToolbarVisible()
                //recordingToolbar.hideFloatingActionButton();
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.getItem(0)
        item.setIcon(R.drawable.ic_learn)
        return true
    }

    /**
     * sets that the learn phase has already been gone through once
     * and the recording button can be shown from the beginning
     */
    private fun setIfLearnHasBeenWatched() {
        val recordFile = File(recordFilePath)
        if (recordFile.exists()) {
            setVolumeSwitchAndFloatingButtonVisible()
            val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
            volumeSwitch.isChecked = true
            isWatchedOnce = true
        }
    }

    /**
     * Starts the background music player
     */
    private fun playBackgroundMusic() {
        backgroundPlayer.playAudio()
    }

    /**
     * Sets the array list for all the jump points that the background music has to make
     */
    private fun setBackgroundAudioJumps() {
        var audioStartValue = 0
        backgroundAudioJumps.add(0, audioStartValue)
        for (k in 0 until story.slides.size) {
            val uri = getStoryUri(story.slides[k].musicFile,story.title)
            audioStartValue += (MediaHelper.getAudioDuration(this,uri) / 1000).toInt()
            backgroundAudioJumps.add(k, audioStartValue)
        }
        backgroundAudioJumps.add(audioStartValue)        //this last one is just added for the copyrights slide
    }

    public override fun onStart() {
        super.onStart()
        //create audio players
        narrationPlayer = AudioPlayer()
        narrationPlayer.onPlayBackStop(MediaPlayer.OnCompletionListener {
            slideNumber++         //move to the next slide
            if (slideNumber < story.slides.size) {     //not at the end of video
                playVideo()
            } else {                            //at the end of video so special case
                makeLogIfNecessary(true)

                videoSeekBar.progress = story.slides.size
                playButton.setImageResource(R.drawable.ic_play_gray)
                setPic(learnImageView)     //sets the pic to the end image
                showStartPracticeSnackBar()
            }
        })
        //recordingToolbar.hideFloatingActionButton();

        backgroundPlayer = AudioPlayer()
        backgroundPlayer.setVolume(BACKGROUND_VOLUME)
        setIfLearnHasBeenWatched()
    }

    private fun markLogStart() {
        startPos = slideNumber
        startTime = System.currentTimeMillis()
    }

    private fun makeLogIfNecessary(request: Boolean = false) {
        if (narrationPlayer.isAudioPlaying || backgroundPlayer.isAudioPlaying
                || request) {
            if (startPos != -1) {
                LearnEntry.saveFilteredLogEntry(startPos, slideNumber,
                        System.currentTimeMillis() - startTime)
                startPos = -1
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        pauseVideo()
        recordingToolbar.onClose()
        recordingToolbar.closeToolbar()
    }

    public override fun onResume() {
        super.onResume()
        //recordingToolbar.hideFloatingActionButton();
    }

    public override fun onStop() {
        super.onStop()
        narrationPlayer.release()
        backgroundPlayer.release()
        recordingToolbar.onClose()
        recordingToolbar.closeToolbar()
        recordingToolbar.releaseToolbarAudio()
    }

    /**
     * Plays the video and runs every time the audio is completed
     */
    internal fun playVideo() {
        setPic(learnImageView)                                                             //set the next image
        val audioFile = AudioFiles.getNarration(storyName, slideNumber)
        //set the next audio
        if (audioFile.exists()) {
            narrationPlayer.setVolume(if (isVolumeOn) 1.0f else 0.0f) //set the volume on or off based on the boolean
            narrationPlayer.setSource(audioFile.getPath())
            narrationPlayer.playAudio()
        }

        videoSeekBar.progress = slideNumber
    }

    /**
     * Button action for playing/pausing the audio
     * @param view button to set listeners for
     */
    fun onClickPlayPauseButton(view: View) {
        if (narrationPlayer.isAudioPlaying) {
            pauseVideo()
        } else {
            markLogStart()

            playButton.setImageResource(R.drawable.ic_pause_gray)

            if (slideNumber >= story.slides.size) {        //reset the video to the beginning because they already finished it
                videoSeekBar.progress = 0
                slideNumber = 0
                playBackgroundMusic()
                playVideo()
            } else {
                resumeVideo()
            }
        }
    }

    /**
     * helper function for pausing the video
     */
    private fun pauseVideo() {
        makeLogIfNecessary()
        narrationPlayer.pauseAudio()
        backgroundPlayer.pauseAudio()
        playButton.setImageResource(R.drawable.ic_play_gray)
    }

    /**
     * helper function for resuming the video
     */
    private fun resumeVideo() {
        if (isFirstTime) {           //actually start playing the video if playVideo() has never been called
            playVideo()
            isFirstTime = false
        } else {
            narrationPlayer.resumeAudio()
            backgroundPlayer.resumeAudio()
            //recordingToolbar.hideFloatingActionButton();
        }
    }

    /**
     * Sets the seekBar listener for the video seek bar
     */
    private fun setSeekBarListener() {
        videoSeekBar.max = story.slides.size      //set the progress bar to have as many markers as images
        videoSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(sBar: SeekBar) {}
            override fun onStartTrackingTouch(sBar: SeekBar) {}
            override fun onProgressChanged(sBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    makeLogIfNecessary()

                    slideNumber = progress
                    narrationPlayer.stopAudio()
                    backgroundPlayer.seekTo(backgroundAudioJumps[slideNumber])
                    if (!backgroundPlayer.isAudioPlaying) {
                        backgroundPlayer.resumeAudio()
                    }
                    if (slideNumber == story.slides.size) {
                        playButton.setImageResource(R.drawable.ic_play_gray)
                        setPic(learnImageView)     //sets the pic to the end image
                        showStartPracticeSnackBar()
                    } else {
                        markLogStart()
                        playVideo()
                        playButton.setImageResource(R.drawable.ic_pause_gray)
                    }

                }
            }
        })
    }

    /**
     * helper function that resets the video to the beginning and turns off the sound
     */
    private fun resetVideoWithSoundOff() {
        playButton.setImageResource(R.drawable.ic_pause_gray)
        videoSeekBar.progress = 0
        slideNumber = 0
        narrationPlayer.setVolume(0.0f)
        val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
        backgroundPlayer.stopAudio()
        volumeSwitch.isChecked = false
        backgroundPlayer.stopAudio()
        backgroundPlayer.setVolume(0.0f)
        playBackgroundMusic()
        isVolumeOn = false

        markLogStart()

        playVideo()
    }

    /**
     * Shows a snackbar at the bottom of the screen to notify the user that they should practice saying the story
     */
    private fun showStartPracticeSnackBar() {
        if (!isWatchedOnce) {
            val snackbar = Snackbar.make(findViewById(R.id.drawer_layout),
                    R.string.learn_phase_practice, Snackbar.LENGTH_INDEFINITE)
            val snackBarView = snackbar.view
            snackBarView.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.lightWhite, null))
            val textView = snackBarView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            snackbar.setAction(R.string.ok) {
                //reset the story with the volume off
                resetVideoWithSoundOff()
                setVolumeSwitchAndFloatingButtonVisible()
                recordingToolbar.keepToolbarVisible()
                //recordingToolbar.hideFloatingActionButton();
            }
            snackbar.show()
        }
        isWatchedOnce = true
    }


    /**
     * Makes the volume switch visible so it can be used
     */
    private fun setVolumeSwitchAndFloatingButtonVisible() {
        //make the floating button visible
        //recordingToolbar.showFloatingActionButton();
        //make the sounds stuff visible
        val soundOff = findViewById<ImageView>(R.id.soundOff)
        val soundOn = findViewById<ImageView>(R.id.soundOn)
        val volumeSwitch = findViewById<Switch>(R.id.volumeSwitch)
        soundOff.visibility = View.VISIBLE
        soundOn.visibility = View.VISIBLE
        volumeSwitch.visibility = View.VISIBLE
        //set the volume switch change listener
        volumeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                narrationPlayer.setVolume(1.0f)
                backgroundPlayer.setVolume(BACKGROUND_VOLUME)
                isVolumeOn = true
            } else {
                narrationPlayer.setVolume(0.0f)
                backgroundPlayer.setVolume(0.0f)
                isVolumeOn = false
            }
        }
    }

    /**
     * This function allows the picture to scale with the phone's screen size.
     *
     * @param aView    The ImageView that will contain the picture.
     */
    private fun setPic(aView: View?) {
        if (aView == null || aView !is ImageView) {
            return
        }

        val slideImage = aView as ImageView?
        var slidePicture: Bitmap? = ImageFiles.getBitmap(storyName, slideNumber)
        if (slideNumber == story.slides.size) {                //gets the end image if we are at the end of the story
            slidePicture = ImageFiles.getBitmap(storyName, ImageFiles.COPYRIGHT)
        }

        if (slidePicture == null) {
            Snackbar.make(rootView, "Could Not Find Picture", Snackbar.LENGTH_SHORT).show()
        }

        //Get the height of the phone.
        val phoneProperties = resources.displayMetrics
        var height = phoneProperties.heightPixels
        val scalingFactor = 0.4
        height = (height * scalingFactor).toInt()

        //scale bitmap
        slidePicture = BitmapScaler.scaleToFitHeight(slidePicture, height)

        //Set the height of the image view
        if(slideImage != null) {
            slideImage.layoutParams.height = height
            slideImage.requestLayout()

            slideImage.setImageBitmap(slidePicture)
        }
    }

    companion object {

        private val BACKGROUND_VOLUME = 0.0f        //makes for no background music but still keeps the functionality in there if we decide to change it later
    }

}