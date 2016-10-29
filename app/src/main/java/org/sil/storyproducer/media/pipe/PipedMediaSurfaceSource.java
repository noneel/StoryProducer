package org.sil.storyproducer.media.pipe;

import android.graphics.Canvas;

import org.sil.storyproducer.media.pipe.PipedMediaSource;

/**
 * Describes a component of the media pipeline which draws frames to a provided canvas when called.
 */
public interface PipedMediaSurfaceSource extends PipedMediaSource {
    /**
     * Request that this component draw a frame to the canvas.
     * @param canv the canvas to be drawn upon.
     * @return the presentation time (in microseconds) of the drawn frame.
     */
    long fillCanvas(Canvas canv);
}
