package com.achugr.coffversation.slack

import com.achugr.coffversation.model.IntroFrequency
import com.slack.api.model.kotlin_extension.block.withBlocks

fun menu() = withBlocks {
    section {
        plainText("Hey! Choose introduction frequency")
    }
    actions {
        staticSelect {
            actionId("start")
            options {
                option {
                    plainText("Now")
                    value(IntroFrequency.NOW_ONCE.name)
                }
                option {
                    plainText("Every monday")
                    value(IntroFrequency.MONDAY_ONCE_A_WEEK.name)
                }
                option {
                    plainText("Every two mondays")
                    value(IntroFrequency.MONDAY_ONCE_TWO_WEEKS.name)
                }
                option {
                    plainText("Disable introductions")
                    value(IntroFrequency.PAUSED.name)
                }
                option {
                    plainText("Every minute (testing)")
                    value(IntroFrequency.EVERY_MINUTE_TESTING.name)
                }
            }
            confirm {
                title("Confirm action")
                plainText("Start introductions")
            }
        }
    }
}