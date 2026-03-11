package com.seclass.stunner.model

/**
 * 3x3 grid of goal zones.
 *
 *  ┌──────────┬──────────┬──────────┐
 *  │ TOP_LEFT │ TOP_CTR  │ TOP_RIGHT│
 *  ├──────────┼──────────┼──────────┤
 *  │ MID_LEFT │ MID_CTR  │ MID_RIGHT│
 *  ├──────────┼──────────┼──────────┤
 *  │ BOT_LEFT │ BOT_CTR  │ BOT_RIGHT│
 *  └──────────┴──────────┴──────────┘
 *
 *  Row 0 = top of goal, Row 2 = bottom of goal
 *  Col 0 = left post,   Col 2 = right post
 */
enum class GoalZone {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MID_LEFT, MID_CENTER, MID_RIGHT,
    BOT_LEFT, BOT_CENTER, BOT_RIGHT;

    companion object {
        /** Map normalized ball position (cx, cy) in [0,1] to a zone. */
        fun fromNormalized(cx: Float, cy: Float): GoalZone {
            val col = when {
                cx < 0.33f -> 0
                cx < 0.67f -> 1
                else       -> 2
            }
            val row = when {
                cy < 0.33f -> 0
                cy < 0.67f -> 1
                else       -> 2
            }
            return values()[row * 3 + col]
        }
    }
}
