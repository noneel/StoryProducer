package org.sil.storyproducer.androidtest.happypath

import android.support.v7.widget.AppCompatSeekBar
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Test
import org.sil.storyproducer.R
import org.sil.storyproducer.androidtest.utilities.ActivityAccessor
import org.sil.storyproducer.androidtest.utilities.AnimationsToggler
import org.sil.storyproducer.androidtest.utilities.Constants
import org.sil.storyproducer.androidtest.utilities.PhaseNavigator
import org.sil.storyproducer.model.Workspace

class VoiceStudioPhaseTest : SwipablePhaseTestBase() {

    override fun navigateToPhase() {
        PhaseNavigator.navigateFromRegistrationScreenToVoiceStudioPhase()
    }

    @Test
    fun should_beAbleToSwipeToNextPhase() {
        swipeUpOnSlide()
        expectToBeOnPhase(Constants.Phase.finalize)
    }

    @Test
    fun should_beAbleToPlaySlideAudio() {
        // Arrange
        makeSureAnAudioClipIsAvailable()
        approveSlides()
        val originalProgress = getCurrentSlideAudioProgress()

        // Act
        pressPlayPauseButton()
        giveAppTimeToPlayAudio()
        pressPlayPauseButton()

        // Assert
        val endingProgress = getCurrentSlideAudioProgress()
        Assert.assertTrue("Expected ending progress to be greater than original progress.", endingProgress > originalProgress)
    }

    @Test
    fun should_beAbleToSwipeBetweenSlides() {
        testSwipingBetweenSlides()
    }

    @Test
    fun should_beAbleToRecordSequentialAudioSnippetsAsOneClip() {
        approveSlides()

        verifyThatRecordingMultipleSnippetsDoesNotCreateMultipleClips()

        openRecordingsListDialog()
        val countOfClipsAfterRecordingSecondSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()

        verifyThatFinalizingOneClipThenRecordingANewSnippetCreatesANewClip(countOfClipsAfterRecordingSecondSnippet)
    }

    private fun verifyThatRecordingMultipleSnippetsDoesNotCreateMultipleClips(): Int? {
        // record a snippet of audio to the clip
        recordAVoiceStudioTranslationSnippet()
        openRecordingsListDialog()
        val countOfClipsAfterRecordingFirstSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()
        // record another snippet to the clip
        recordAVoiceStudioTranslationSnippet()
        // 'finalize' the clip, which should prevent additional snippets from being appended to it.
        clickStopClipButton()
        openRecordingsListDialog()
        val countOfClipsAfterRecordingSecondSnippet = getCurrentCountOfClips()
        closeRecordingsListDialog()

        Assert.assertEquals(countOfClipsAfterRecordingFirstSnippet, countOfClipsAfterRecordingSecondSnippet)
        return countOfClipsAfterRecordingSecondSnippet
    }

    private fun verifyThatFinalizingOneClipThenRecordingANewSnippetCreatesANewClip(countOfClipsAfterRecordingSecondSnippet: Int?) {
        // record a snippet of audio to the clip
        recordAVoiceStudioTranslationSnippet()
        // 'finalize' the clip, which should prevent additional snippets from being appended to it.
        clickStopClipButton()
        openRecordingsListDialog()
        val numberOfClipsAfterRecordingASnippetForANewClip = getCurrentCountOfClips()
        closeRecordingsListDialog()

        Assert.assertEquals(countOfClipsAfterRecordingSecondSnippet!! + 1, numberOfClipsAfterRecordingASnippetForANewClip)
    }

    private fun clickStopClipButton() {
        Espresso.onView(allOf(withId(R.id.finish_recording_button), isDisplayed())).perform(click())
    }

    private fun openRecordingsListDialog() {
        Espresso.onView(allOf(withId(R.id.list_recordings_button), isDisplayed())).perform(click())
    }

    private fun closeRecordingsListDialog() {
        Espresso.onView(allOf(withId(R.id.exitButton), isDisplayed())).perform(click())
    }

    private fun getCurrentCountOfClips(): Int? {
        val numberOfClips = arrayOfNulls<Int>(1)
        Espresso.onView(CoreMatchers.allOf(ViewMatchers.withId(R.id.recordings_list), ViewMatchers.isDisplayed())).perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "Gets the currently displayed activity so that Espresso tests can reach 'under the hood' and reference actual Views."
            }

            override fun perform(uiController: UiController, view: View) {
                numberOfClips[0] = (view as RecyclerView).childCount
            }
        })
        return numberOfClips[0]
    }

    private fun makeSureAnAudioClipIsAvailable() {
        selectPhase(Constants.Phase.translate)
        if (!areThereAnyAudioClipsOnThisSlide()) {
            recordAnAudioTranslationClip()
        }
        selectPhase(Constants.Phase.voiceStudio)
    }

    private fun areThereAnyAudioClipsOnThisSlide(): Boolean {
        val showRecordingsListButton = ActivityAccessor.getCurrentActivity()?.findViewById<ImageButton>(org.sil.storyproducer.R.id.list_recordings_button)
        return showRecordingsListButton?.visibility != View.INVISIBLE
    }

    private fun recordAnAudioTranslationClip() {
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            Thread.sleep(Constants.durationToRecordTranslatedClip)
            pressMicButton()
        }
    }

    private fun recordAVoiceStudioTranslationSnippet() {
        AnimationsToggler.withoutCustomAnimations {
            pressMicButton()
            Thread.sleep(Constants.durationToRecordVoiceStudioClip)
            pressMicButton()
        }
    }

    private fun pressMicButton() {
        Espresso.onView(allOf(withId(R.id.start_recording_button), isDisplayed())).perform(click())
    }

    private fun getCurrentSlideAudioProgress(): Int {
        val progressBar = ActivityAccessor.getCurrentActivity()?.findViewById<AppCompatSeekBar>(org.sil.storyproducer.R.id.videoSeekBar)
        return progressBar!!.progress
    }

    private fun pressPlayPauseButton() {
        Espresso.onView(allOf(withId(R.id.fragment_reference_audio_button), isDisplayed())).perform(click())
    }

    private fun giveAppTimeToPlayAudio() {
        Thread.sleep(Constants.durationToPlayTranslatedClip)
    }

    private fun approveSlides() {
        selectPhase(Constants.Phase.accuracyCheck)
        for (item in Workspace.activeStory.slides) {
            item.isChecked = true;
        }
        Workspace.activeStory.isApproved = true
        selectPhase(Constants.Phase.voiceStudio)
    }

    private fun expectToBeOnPhase(phase: String) {
        Espresso.onView(allOf(ViewMatchers.withText(phase))).check(matches(isDisplayed()))
    }
}