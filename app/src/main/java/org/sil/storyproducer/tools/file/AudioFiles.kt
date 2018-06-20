package org.sil.storyproducer.tools.file

import android.content.Context
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.TemplateSlide
import org.sil.storyproducer.tools.StorySharedPreferences


import java.io.File
import java.io.InputStream
import java.nio.file.FileSystem
import java.util.ArrayList

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */
object AudioFiles {
    private val PREFER_EXTENSION = ".m4a"

    private val DRAFT_AUDIO_PREFIX = "translation"
    private val COMMENT_PREFIX = "comment"
    private val DRAMATIZATION_AUDIO_PREFIX = "dramatization"

    private val BACKTRANSLATION_AUDIO_PREFIX = "backtranslation"

    private enum class ModalType {
        DRAFT, COMMUNITY, DRAMATIZATION, BACKTRANSLATION, WHOLESTORY
    }

    enum class RenameCode {
        SUCCESS,
        ERROR_LENGTH,
        ERROR_SPECIAL_CHARS,
        ERROR_UNDEFINED
    }



    //*** WSBT ***
    fun getWholeStory(story: String): File {
        return File(FileSystem.getProjectDirectory(story), WHOLESTORY_AUDIO_PREFIX + PREFER_EXTENSION)
    }

    @JvmOverloads
    fun getDraft(story: String, slide: Int, draftTitle: String = StorySharedPreferences.getDraftForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), DRAFT_AUDIO_PREFIX + slide + "_" + draftTitle + PREFER_EXTENSION)
    }

    fun getDraftTitle(file: File): String {
        val filename = file.name
        return getTitleFromPath(filename, DRAFT_AUDIO_PREFIX, PREFER_EXTENSION)
    }

    fun allDraftsComplete(story: String, slideCount: Int): Boolean {
        for (i in 0 until slideCount) {
            val draftAudio = getDraft(story, i)
            if (!draftAudio.exists()) {
                return false
            }
        }
        return true
    }

    /**
     * deletes the designated audio draft
     * @param story the story the draft comes from
     * @param slide the slide the draft comes from
     * @param draftTitle the name of the draft in question
     */
    fun deleteDraft(story: String, slide: Int, draftTitle: String) {
        val file = getDraft(story, slide, draftTitle)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * renames the designated audio draft if the new name is valid and the file exists
     * @param story the story the draft comes from
     * @param slide the slide of the story the draft comes from
     * @param oldTitle the old title of the draft
     * @param newTitle the proposed new title for the draft
     * @return returns success or error code of renaming
     */
    fun renameDraft(story: String, slide: Int, oldTitle: String, newTitle: String): RenameCode {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.DRAFT, PREFER_EXTENSION)
    }

    /**
     * Returns a list of draft titles for the story and slide in question
     * @param story the story where the drafts come from
     * @param slide the slide where the drafts come from
     * @return the array of draft titles
     */
    fun getDraftTitles(story: String, slide: Int): Array<String> {
        return getRecordingTitlesHelper(story, slide, DRAFT_AUDIO_PREFIX, PREFER_EXTENSION)
    }

    //*** Community Check ***

    fun getComment(story: String, slide: Int, commentTitle: String): File {
        return File(FileSystem.getProjectDirectory(story), COMMENT_PREFIX + slide + "_" + commentTitle + PREFER_EXTENSION)
    }

    /**
     * deletes the designated audio comment
     * @param story the story the comment comes from
     * @param slide the slide the comment comes from
     * @param commentTitle the name of the comment in question
     */
    fun deleteComment(story: String, slide: Int, commentTitle: String) {
        val file = getComment(story, slide, commentTitle)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * renames the designated audio comment if the new name is valid and the file exists
     * @param story the story the comment comes from
     * @param slide the slide of the story the comment comes from
     * @param oldTitle the old title of the comment
     * @param newTitle the proposed new title for the comment
     * @return returns success or error code of renaming
     */
    fun renameComment(story: String, slide: Int, oldTitle: String, newTitle: String): RenameCode {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.COMMUNITY, PREFER_EXTENSION)
    }

    /**
     * Returns a list of comment titles for the story and slide in question
     * @param story the story where the comments come from
     * @param slide the slide where the comments come from
     * @return the array of comment titles
     */
    fun getCommentTitles(story: String, slide: Int): Array<String> {
        return getRecordingTitlesHelper(story, slide, COMMENT_PREFIX, PREFER_EXTENSION)
    }

    @JvmOverloads
    fun getBackTranslation(story: String, slide: Int, backTitle: String = StorySharedPreferences.getBackTranslationForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), BACKTRANSLATION_AUDIO_PREFIX + slide + "_" + backTitle + WAV_EXTENSION)
    }

    fun getBackTranslationTemp(story: String): File {
        return File(FileSystem.getHiddenTempDirectory(story), BACKTRANSLATION_AUDIO_PREFIX + "_" + "T" + WAV_EXTENSION)
    }

    fun getBackTranslationTitle(file: File): String {
        val filename = file.name
        return getTitleFromPath(filename, BACKTRANSLATION_AUDIO_PREFIX, WAV_EXTENSION)
    }


    /**
     * deletes the designated audio dramatization
     * @param story the story the dramatization comes from
     * @param slide the slide the dramatization comes from
     * @param draftTitle the name of the dramatization in question
     */
    fun deleteBackTranslation(story: String, slide: Int, draftTitle: String) {
        val file = getBackTranslation(story, slide, draftTitle)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * renames the designated audio dramatization if the new name is valid and the file exists
     * @param story the story the dramatization comes from
     * @param slide the slide of the story the dramatization comes from
     * @param oldTitle the old title of the dramatization
     * @param newTitle the proposed new title for the dramatization
     * @return returns success or error code of renaming
     */
    fun renameBackTranslation(story: String, slide: Int, oldTitle: String, newTitle: String): RenameCode {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.BACKTRANSLATION, PREFER_EXTENSION)
    }

    /**
     * Returns a list of dramatization titles for the story and slide in question
     * @param story the story where the dramatization come from
     * @param slide the slide where the dramatization come from
     * @return the array of dramatization titles
     */
    fun getBackTranslationTitles(story: String, slide: Int): Array<String> {
        return getRecordingTitlesHelper(story, slide, BACKTRANSLATION_AUDIO_PREFIX, WAV_EXTENSION)
    }

    @JvmOverloads
    fun getDramatization(story: String, slide: Int, dramaTitle: String = StorySharedPreferences.getDramatizationForSlideAndStory(slide, story)): File {
        return File(FileSystem.getProjectDirectory(story), DRAMATIZATION_AUDIO_PREFIX + slide + "_" + dramaTitle + WAV_EXTENSION)
    }

    fun getDramatizationTemp(story: String): File {
        return File(FileSystem.getHiddenTempDirectory(story), DRAMATIZATION_AUDIO_PREFIX + "_" + "T" + WAV_EXTENSION)
    }

    fun getDramatizationTitle(file: File): String {
        val filename = file.name
        return getTitleFromPath(filename, DRAMATIZATION_AUDIO_PREFIX, WAV_EXTENSION)
    }

    /**
     * deletes the designated audio dramatization
     * @param story the story the dramatization comes from
     * @param slide the slide the dramatization comes from
     * @param draftTitle the name of the dramatization in question
     */
    fun deleteDramatization(story: String, slide: Int, draftTitle: String) {
        val file = getDramatization(story, slide, draftTitle)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * renames the designated audio dramatization if the new name is valid and the file exists
     * @param story the story the dramatization comes from
     * @param slide the slide of the story the dramatization comes from
     * @param oldTitle the old title of the dramatization
     * @param newTitle the proposed new title for the dramatization
     * @return returns success or error code of renaming
     */
    fun renameDramatization(story: String, slide: Int, oldTitle: String, newTitle: String): RenameCode {
        return renameAudioFileHelper(story, slide, oldTitle, newTitle, ModalType.DRAMATIZATION, PREFER_EXTENSION)
    }

    /**
     * Returns a list of dramatization titles for the story and slide in question
     * @param story the story where the dramatization come from
     * @param slide the slide where the dramatization come from
     * @return the array of dramatization titles
     */
    fun getDramatizationTitles(story: String, slide: Int): Array<String> {
        return getRecordingTitlesHelper(story, slide, DRAMATIZATION_AUDIO_PREFIX, WAV_EXTENSION)
    }

    //**** Helpers ***//
    private fun renameAudioFileHelper(story: String, slide: Int, oldTitle: String, newTitle: String, type: ModalType, extension: String): RenameCode {
        // Requirements for file names:
        //        - must be under 20 characters
        //        - must be only contain alphanumeric characters or spaces/underscores
        if (newTitle.length > 20) {
            return RenameCode.ERROR_LENGTH
        }
        if (!newTitle.matches("[A-Za-z0-9\\s_]+".toRegex())) {
            return RenameCode.ERROR_SPECIAL_CHARS
        }
        var file = getComment(story, slide, oldTitle)
        when (type) {
        //set the file based on the different file types
            AudioFiles.ModalType.DRAFT -> file = getDraft(story, slide, oldTitle)
            AudioFiles.ModalType.COMMUNITY -> file = getComment(story, slide, oldTitle)
            AudioFiles.ModalType.DRAMATIZATION -> file = getDramatization(story, slide, oldTitle)
            AudioFiles.ModalType.BACKTRANSLATION -> file = getBackTranslation(story, slide, oldTitle)
        }

        var renamed = false
        if (file.exists()) {
            val newPathName = file.absolutePath.replace(oldTitle + extension, newTitle + extension)
            val newFile = File(newPathName)
            if (!newFile.exists()) {
                renamed = file.renameTo(newFile)
            }
        }
        return if (renamed) {
            RenameCode.SUCCESS
        } else {
            RenameCode.ERROR_UNDEFINED
        }
    }

    private fun getRecordingTitlesHelper(story: String, slide: Int, prefix: String, extension: String): Array<String> {
        val titles = ArrayList<String>()
        val storyDirectory = FileSystem.getProjectDirectory(story)
        val storyDirectoryFiles = storyDirectory.listFiles()
        for (storyDirectoryFile in storyDirectoryFiles) {
            val filename = storyDirectoryFile.getName()
            if (filename.startsWith(prefix + slide + "_") && filename.endsWith(extension)) {
                titles.add(getTitleFromPath(filename, prefix, extension))
            }
        }
        val returnTitlesArray = arrayOfNulls<String>(titles.size)
        return titles.toTypedArray()
    }

    /**
     * Extract title from path.
     */
    private fun getTitleFromPath(filename: String, prefix: String, extension: String): String {
        //Note: Assume no dots in filename.
        return filename.replaceFirst("$prefix\\d+_".toRegex(), "").replace(extension, "")
    }

}//*** Draft ***
//*** Consultant Check ***
//*** Back Translation ***
//*** Dramatization ***