package ca.on.hojat.mlkitdemo.shared

/**
 * Describes a frame info.
 */
class FrameMetadata(
    val width: Int,
    val height: Int,
    val rotation: Int
) {

    /**
     * Builder of {@link FrameMetadata}.
     */
    class Builder {
        private var width = 0
        private var height = 0
        private var rotation = 0

        fun setWidth(width: Int): Builder {
            this.width = width
            return this
        }

        fun setHeight(height: Int): Builder {
            this.height = height
            return this
        }

        fun setRotation(rotation: Int): Builder {
            this.rotation = rotation
            return this
        }

        fun build() = FrameMetadata(width, height, rotation)

    }
}
