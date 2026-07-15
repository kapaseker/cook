package page.chat

import androidx.compose.runtime.Composable
import cook.generated.resources.Res
import cook.generated.resources.agent_request_failed
import cook.generated.resources.could_not_answer
import cook.generated.resources.empty_response
import cook.generated.resources.missing_api_key
import cook.generated.resources.thinking
import cook.generated.resources.unsupported_platform
import cook.generated.resources.welcome_message
import org.jetbrains.compose.resources.stringResource

data class ChatStrings(
    val welcomeMessage: String,
    val thinking: String,
    val emptyResponse: String,
    val agentRequestFailed: String,
    val couldNotAnswer: String,
    val missingApiKey: String,
    val unsupportedPlatform: String,
)

@Composable
internal fun chatStrings() = ChatStrings(
    welcomeMessage = stringResource(Res.string.welcome_message),
    thinking = stringResource(Res.string.thinking),
    emptyResponse = stringResource(Res.string.empty_response),
    agentRequestFailed = stringResource(Res.string.agent_request_failed),
    couldNotAnswer = stringResource(Res.string.could_not_answer),
    missingApiKey = stringResource(Res.string.missing_api_key),
    unsupportedPlatform = stringResource(Res.string.unsupported_platform),
)
