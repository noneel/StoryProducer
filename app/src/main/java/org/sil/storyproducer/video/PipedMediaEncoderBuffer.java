package org.sil.storyproducer.video;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

public class PipedMediaEncoderBuffer extends PipedMediaCodecBuffer {
    private MediaFormat mConfigureFormat;
    private MediaFormat mSourceFormat;

    public PipedMediaEncoderBuffer(MediaFormat format) {
        mConfigureFormat = format;
    }

    @Override
    public MediaHelper.MediaType getType() {
        return MediaHelper.getTypeFromFormat(mConfigureFormat);
    }

    @Override
    public void setup() throws IOException {
        mSource.setup();
        mSourceFormat = mSource.getFormat();

        //audio keys
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CHANNEL_COUNT);
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_SAMPLE_RATE);

        //video keys
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_WIDTH);
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_HEIGHT);
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_COLOR_FORMAT);
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_FRAME_RATE);
        MediaHelper.copyMediaFormatIntKey(mSourceFormat, mConfigureFormat, MediaFormat.KEY_CAPTURE_RATE);

        //TODO: Make buffers appropriate size
        //encoder input buffers are too small by default
        mConfigureFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MediaHelper.MAX_INPUT_BUFFER_SIZE);

        mCodec = MediaCodec.createEncoderByType(mConfigureFormat.getString(MediaFormat.KEY_MIME));
        mCodec.configure(mConfigureFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        start();
    }

    @Override
    protected String getComponentName() {
        return "encoder";
    }
}
