package com.larpers.upmhackitup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.larpers.upmhackitup.ui.theme.UPMHackItUpTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class OnboardingStage {
    Splash,
    Welcome,
    Tour
}

private data class WelcomeSlide(
    val id: Int,
    @param:DrawableRes val imageRes: Int
)

private enum class PillPosition {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    Center
}

private data class FloatingPill(
    val label: String,
    val position: PillPosition
)

private data class TourSlide(
    val id: String,
    val heading: String,
    val pills: List<FloatingPill>
)

private data class TourLayoutMetrics(
    val cardMaxWidth: Int,
    val headingTopPadding: Int,
    val headingFontSize: Float,
    val headingLineHeight: Float,
    val progressBottomPadding: Int
)

private val BackgroundTop = Color(0xFFEEF1FB)
private val BackgroundMiddle = Color(0xFFE6ECFA)
private val BackgroundBottom = Color(0xFFDDE5F7)
private val Ink = Color(0xFF0C1A3D)
private val InkMuted = Color(0xFF64748B)
private val Brand = Color(0xFF2A3FD9)
private val BrandTrack = Color(0x262A3FD9)
private val GlassWhite = Color(0x66FFFFFF)
private val GlassBorder = Color(0x99FFFFFF)
private val GlassShadow = Color(0x260C1A3D)

private val WelcomeSlides = listOf(
    WelcomeSlide(1, R.drawable.claro_carousel_1),
    WelcomeSlide(2, R.drawable.claro_carousel_2),
    WelcomeSlide(3, R.drawable.claro_carousel_3),
    WelcomeSlide(4, R.drawable.claro_carousel_4)
)

private val TourSlides = listOf(
    TourSlide(
        id = "plan",
        heading = "Ready to succeed?\nin one place",
        pills = listOf(
            FloatingPill("Today", PillPosition.TopLeft),
            FloatingPill("3 tasks", PillPosition.TopRight)
        )
    ),
    TourSlide(
        id = "ship",
        heading = "Ship faster, with\nless back-and-forth",
        pills = listOf(
            FloatingPill("In review", PillPosition.TopRight),
            FloatingPill("Ready to ship", PillPosition.BottomLeft)
        )
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UPMHackItUpTheme(darkTheme = false, dynamicColor = false) {
                ClaroOnboardingApp()
            }
        }
    }
}

@Composable
private fun ClaroOnboardingApp() {
    var stageName by rememberSaveable { mutableStateOf(OnboardingStage.Splash.name) }
    var tourIndex by rememberSaveable { mutableIntStateOf(0) }
    val stage = OnboardingStage.valueOf(stageName)
    val isLastTour = tourIndex == TourSlides.lastIndex

    val primaryLabel = when (stage) {
        OnboardingStage.Splash -> ""
        OnboardingStage.Welcome -> "Get started"
        OnboardingStage.Tour -> if (isLastTour) "Create account" else "Continue"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom)
                )
            )
    ) {
        BackHandler(enabled = stage == OnboardingStage.Tour) {
            if (tourIndex == 0) {
                stageName = OnboardingStage.Welcome.name
            } else {
                tourIndex -= 1
            }
        }

        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                val movingForward = targetState.ordinal >= initialState.ordinal
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> if (movingForward) fullWidth / 5 else -fullWidth / 5 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> if (movingForward) -fullWidth / 6 else fullWidth / 6 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "onboardingStageTransition"
        ) { animatedStage ->
            when (animatedStage) {
                OnboardingStage.Splash -> SplashScreen(
                    onAnimationComplete = { stageName = OnboardingStage.Welcome.name }
                )

                OnboardingStage.Welcome -> OnboardingScreenScaffold(
                    primaryLabel = primaryLabel,
                    onPrimaryClick = {
                        tourIndex = 0
                        stageName = OnboardingStage.Tour.name
                    }
                ) {
                    WelcomeScreen()
                }

                OnboardingStage.Tour -> OnboardingScreenScaffold(
                    primaryLabel = primaryLabel,
                    onPrimaryClick = {
                        if (isLastTour) {
                            // Completion route not wired yet.
                        } else {
                            tourIndex += 1
                        }
                    }
                ) {
                    TourScreen(
                        index = tourIndex,
                        total = TourSlides.size,
                        onBack = {
                            if (tourIndex == 0) {
                                stageName = OnboardingStage.Welcome.name
                            } else {
                                tourIndex -= 1
                            }
                        },
                        onNext = {
                            if (tourIndex < TourSlides.lastIndex) {
                                tourIndex += 1
                            }
                        },
                        onSkip = {
                            // Skip remains visible to match the Claro reference.
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreenScaffold(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val compactHeight = configuration.screenHeightDp < 820
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val footerHeightDp = remember(footerHeightPx, density) {
        with(density) { footerHeightPx.toDp() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(
                    top = if (compactHeight) 12.dp else 24.dp,
                    bottom = footerHeightDp + if (compactHeight) 16.dp else 24.dp
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }

        BottomActions(
            primaryLabel = primaryLabel,
            onPrimaryClick = onPrimaryClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeightPx = it.height }
        )
    }
}

@Composable
private fun SplashScreen(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animateIn by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (animateIn) 0f else 30f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "splashTranslate"
    )

    LaunchedEffect(Unit) {
        animateIn = true
        delay(3500)
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = contentAlpha
                translationY = translateY
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.claro_logo),
                contentDescription = "Claro",
                modifier = Modifier.size(128.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Claro",
                    color = Ink,
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                )
                Text(
                    text = "Create together, beautifully",
                    color = InkMuted,
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.SansSerif
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 820
    val veryCompactHeight = configuration.screenHeightDp < 740

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.claro_logo),
            contentDescription = "Claro",
            modifier = Modifier
                .padding(bottom = if (compactHeight) 16.dp else 24.dp)
                .size(if (compactHeight) 56.dp else 64.dp)
        )

        WelcomeCarousel(
            slides = WelcomeSlides,
            compactHeight = compactHeight,
            veryCompactHeight = veryCompactHeight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (compactHeight) 20.dp else 32.dp)
        )

        Text(
            text = "Create together,\nbeautifully.",
            color = Ink,
            textAlign = TextAlign.Center,
            style = headingTextStyle(
                fontSize = when {
                    veryCompactHeight -> 32.sp
                    compactHeight -> 36.sp
                    else -> 40.sp
                },
                lineHeight = when {
                    veryCompactHeight -> 36.sp
                    compactHeight -> 40.sp
                    else -> 46.sp
                }
            )
        )

        Text(
            text = "Claro helps teams plan, create, and ship amazing work.",
            modifier = Modifier
                .padding(top = if (compactHeight) 12.dp else 16.dp)
                .widthIn(max = if (compactHeight) 260.dp else 280.dp),
            color = InkMuted,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = if (compactHeight) 14.sp else 15.sp,
                lineHeight = if (compactHeight) 22.sp else 24.sp
            )
        )
    }
}

@Composable
private fun WelcomeCarousel(
    slides: List<WelcomeSlide>,
    compactHeight: Boolean,
    veryCompactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var carouselWidthPx by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    when {
                        veryCompactHeight -> 220.dp
                        compactHeight -> 244.dp
                        else -> 280.dp
                    }
                )
                .onSizeChanged { carouselWidthPx = it.width.toFloat() }
                .pointerInput(slides.size, carouselWidthPx) {
                    val scrubStepPx = (carouselWidthPx * 0.29f).coerceAtLeast(1f)
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetPx += dragAmount

                            while (dragOffsetPx <= -scrubStepPx) {
                                dragOffsetPx += scrubStepPx
                                currentIndex = (currentIndex + 1) % slides.size
                            }

                            while (dragOffsetPx >= scrubStepPx) {
                                dragOffsetPx -= scrubStepPx
                                currentIndex = (currentIndex - 1 + slides.size) % slides.size
                            }
                        },
                        onDragEnd = {
                            val scrubProgress = (-dragOffsetPx / scrubStepPx).coerceIn(-1f, 1f)
                            currentIndex = when {
                                scrubProgress >= 0.35f -> (currentIndex + 1) % slides.size
                                scrubProgress <= -0.35f -> (currentIndex - 1 + slides.size) % slides.size
                                else -> currentIndex
                            }
                            dragOffsetPx = 0f
                        },
                        onDragCancel = {
                            dragOffsetPx = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val maxWidthDp = maxWidth
            val baseOffsetDp = maxWidthDp * 0.29f
            val scrubStepPx = (carouselWidthPx * 0.29f).coerceAtLeast(1f)
            val scrubProgress = (-dragOffsetPx / scrubStepPx).coerceIn(-1f, 1f)
            val visualScrubProgress = welcomeCarouselVisualScrubProgress(scrubProgress)
            val centerCardWidth = when {
                veryCompactHeight -> 150.dp
                compactHeight -> 164.dp
                else -> 180.dp
            }
            val sideCardWidth = when {
                veryCompactHeight -> 122.dp
                compactHeight -> 134.dp
                else -> 145.dp
            }
            val centerCardHeight = when {
                veryCompactHeight -> 214.dp
                compactHeight -> 236.dp
                else -> 260.dp
            }
            val sideCardHeight = when {
                veryCompactHeight -> 188.dp
                compactHeight -> 208.dp
                else -> 230.dp
            }

            listOf(-2, -1, 0, 1, 2).forEach { offset ->
                val slideIndex = Math.floorMod(currentIndex + offset, slides.size)
                val slide = slides[slideIndex]
                val effectiveOffset = offset - visualScrubProgress
                val absoluteOffset = abs(effectiveOffset)
                val isVisible = absoluteOffset <= 2.15f
                if (!isVisible) {
                    return@forEach
                }

                val direction = when {
                    effectiveOffset < 0f -> -1f
                    effectiveOffset > 0f -> 1f
                    else -> 0f
                }
                val translationDp = baseOffsetDp *
                    welcomeCarouselOffsetMultiplier(absoluteOffset) *
                    direction
                val centerProgress = absoluteOffset.coerceIn(0f, 1f)
                val scale = welcomeCarouselLerp(
                    absoluteOffset = absoluteOffset,
                    centerValue = 1f,
                    nearValue = 0.9f,
                    farValue = 0.8f
                )
                val rotationY = -direction * 12f * absoluteOffset.coerceIn(0f, 1f)
                val brightness = welcomeCarouselLerp(
                    absoluteOffset = absoluteOffset,
                    centerValue = 1f,
                    nearValue = 0.92f,
                    farValue = 0.84f
                )
                val cardWidth = lerp(centerCardWidth, sideCardWidth, centerProgress)
                val cardHeight = lerp(centerCardHeight, sideCardHeight, centerProgress)
                val cornerRadius = lerp(20.dp, 18.dp, centerProgress)
                val shadowElevation = lerp(18.dp, 8.dp, centerProgress)
                val cardShape = RoundedCornerShape(cornerRadius)
                val glowAlpha = welcomeCarouselLerp(
                    absoluteOffset = absoluteOffset,
                    centerValue = 0.36f,
                    nearValue = 0.22f,
                    farValue = 0.12f
                )
                val borderAlpha = welcomeCarouselLerp(
                    absoluteOffset = absoluteOffset,
                    centerValue = 0.72f,
                    nearValue = 0.5f,
                    farValue = 0.32f
                )

                Box(
                    modifier = Modifier
                        .offset(x = translationDp)
                        .zIndex(10f - absoluteOffset)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.rotationY = rotationY
                            alpha = welcomeCarouselLerp(
                                absoluteOffset = absoluteOffset,
                                centerValue = 1f,
                                nearValue = 0.96f,
                                farValue = 0.82f
                            )
                        }
                        .width(cardWidth)
                        .height(cardHeight)
                        .shadow(
                            elevation = shadowElevation + 10.dp,
                            shape = cardShape,
                            clip = false,
                            ambientColor = Brand.copy(alpha = glowAlpha),
                            spotColor = Brand.copy(alpha = glowAlpha)
                        )
                        .shadow(
                            elevation = shadowElevation,
                            shape = cardShape,
                            clip = false,
                            ambientColor = Brand.copy(alpha = 0.28f),
                            spotColor = Brand.copy(alpha = 0.28f)
                        )
                        .clip(cardShape)
                        .background(Color(0xFF7C8CA6))
                        .clickable {
                            currentIndex = when {
                                effectiveOffset < -0.25f -> (currentIndex - 1 + slides.size) % slides.size
                                effectiveOffset > 0.25f -> (currentIndex + 1) % slides.size
                                else -> currentIndex
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = slide.imageRes),
                        contentDescription = "Carousel slide ${slide.id}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = brightness
                        },
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = borderAlpha),
                                        Brand.copy(alpha = borderAlpha * 0.55f),
                                        Color.White.copy(alpha = borderAlpha * 0.78f)
                                    )
                                ),
                                shape = cardShape
                            )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.forEachIndexed { index, slide ->
                val isActive = currentIndex == index
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { currentIndex = index }
                        .background(if (isActive) Brand else Brand.copy(alpha = 0.25f))
                        .width(if (isActive) 24.dp else 8.dp)
                        .height(8.dp)
                )
            }
        }
    }
}

private fun welcomeCarouselVisualScrubProgress(scrubProgress: Float): Float {
    val direction = when {
        scrubProgress < 0f -> -1f
        scrubProgress > 0f -> 1f
        else -> 0f
    }
    val amount = abs(scrubProgress).coerceIn(0f, 1f)
    val easedAmount = 1f - ((1f - amount) * (1f - amount))

    return direction * easedAmount
}

private fun welcomeCarouselOffsetMultiplier(absoluteOffset: Float): Float =
    when {
        absoluteOffset <= 1f -> absoluteOffset
        absoluteOffset <= 2f -> 1f + ((absoluteOffset - 1f) * 0.9f)
        else -> 1.9f + ((absoluteOffset - 2f) * 0.45f)
    }

private fun welcomeCarouselLerp(
    absoluteOffset: Float,
    centerValue: Float,
    nearValue: Float,
    farValue: Float
): Float =
    when {
        absoluteOffset <= 1f -> lerpFloat(centerValue, nearValue, absoluteOffset)
        else -> lerpFloat(nearValue, farValue, (absoluteOffset - 1f).coerceIn(0f, 1f))
    }

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + ((stop - start) * fraction.coerceIn(0f, 1f))

@Composable
private fun TourScreen(
    index: Int,
    total: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetPx by remember(index) { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 72f
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val tourMetrics = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density.fontScale
    ) {
        responsiveTourMetrics(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            fontScale = density.fontScale
        )
    }
    val tourHeadingStyle = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        density.fontScale
    ) {
        responsiveTourHeadingStyle(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            fontScale = density.fontScale
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(index, total) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetPx += dragAmount
                    },
                    onDragEnd = {
                        when {
                            dragOffsetPx >= swipeThresholdPx -> onBack()
                            dragOffsetPx <= -swipeThresholdPx -> onNext()
                        }
                        dragOffsetPx = 0f
                    },
                    onDragCancel = {
                        dragOffsetPx = 0f
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(8.dp)
            ) {
                Text(
                    text = "\u2039",
                    color = Ink,
                    style = TextStyle(
                        fontSize = 26.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    )
                )
            }

            Text(
                text = "Skip",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = InkMuted,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = tourMetrics.progressBottomPadding.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(BrandTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((index + 1f) / total.toFloat())
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Brand)
            )
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val movingForward = targetState > initialState
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> if (movingForward) fullWidth / 4 else -fullWidth / 4 }
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    targetOffsetX = { fullWidth -> if (movingForward) -fullWidth / 5 else fullWidth / 5 }
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "tourSlideTransition"
        ) { animatedIndex ->
            val animatedSlide = TourSlides[animatedIndex]

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = tourMetrics.cardMaxWidth.dp)
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = GlassShadow,
                            spotColor = GlassShadow
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFC9D4F5),
                                    Color(0xFFB8C6EF),
                                    Color(0xFF8AA0E4)
                                )
                            )
                        )
                ) {
                    Text(
                        text = "Image slot",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White.copy(alpha = 0.72f),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    )

                    animatedSlide.pills.forEach { pill ->
                        FloatingPill(
                            label = pill.label,
                            position = pill.position
                        )
                    }
                }

                Text(
                    text = animatedSlide.heading,
                    modifier = Modifier
                        .padding(top = tourMetrics.headingTopPadding.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .wrapContentHeight(),
                    color = Ink,
                    textAlign = TextAlign.Center,
                    style = tourHeadingStyle
                )
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.FloatingPill(
    label: String,
    position: PillPosition
) {
    val alignment = when (position) {
        PillPosition.TopLeft -> Alignment.TopStart
        PillPosition.TopRight -> Alignment.TopEnd
        PillPosition.BottomLeft -> Alignment.BottomStart
        PillPosition.BottomRight -> Alignment.BottomEnd
        PillPosition.Center -> Alignment.Center
    }

    val offset = when (position) {
        PillPosition.TopLeft -> androidx.compose.ui.unit.IntOffset(16, 16)
        PillPosition.TopRight -> androidx.compose.ui.unit.IntOffset(-16, 16)
        PillPosition.BottomLeft -> androidx.compose.ui.unit.IntOffset(16, -16)
        PillPosition.BottomRight -> androidx.compose.ui.unit.IntOffset(-16, -16)
        PillPosition.Center -> androidx.compose.ui.unit.IntOffset(0, 0)
    }

    Row(
        modifier = Modifier
            .align(alignment)
            .offset { offset }
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            )
            .clip(CircleShape)
            .background(GlassWhite)
            .border(1.dp, GlassBorder, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Ink,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun BottomActions(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BackgroundBottom.copy(alpha = 0.95f),
                        BackgroundBottom
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassButton(
            label = primaryLabel,
            containerColor = Brand.copy(alpha = 0.85f),
            contentColor = Color.White,
            borderColor = Color.White.copy(alpha = 0.3f),
            onClick = onPrimaryClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        GlassButton(
            label = "I already have an account",
            containerColor = Color.White.copy(alpha = 0.4f),
            contentColor = Ink,
            borderColor = Color.White.copy(alpha = 0.6f),
            onClick = {}
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = legalCopy(),
            modifier = Modifier.padding(horizontal = 18.dp),
            color = InkMuted.copy(alpha = 0.9f),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                lineHeight = 18.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GlassButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = GlassShadow,
                spotColor = GlassShadow
            ),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun headingTextStyle(fontSize: androidx.compose.ui.unit.TextUnit, lineHeight: androidx.compose.ui.unit.TextUnit): TextStyle {
    return TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = (-0.4).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
}

private fun responsiveTourHeadingStyle(
    screenWidthDp: Int,
    screenHeightDp: Int,
    fontScale: Float
): TextStyle {
    val metrics = responsiveTourMetrics(
        screenWidthDp = screenWidthDp,
        screenHeightDp = screenHeightDp,
        fontScale = fontScale
    )

    return headingTextStyle(
        fontSize = metrics.headingFontSize.sp,
        lineHeight = metrics.headingLineHeight.sp
    )
}

private fun responsiveTourMetrics(
    screenWidthDp: Int,
    screenHeightDp: Int,
    fontScale: Float
): TourLayoutMetrics {
    val widthFactor = (screenWidthDp / 390f).coerceIn(0.82f, 1f)
    val effectiveHeight = screenHeightDp / fontScale.coerceAtLeast(1f)

    return when {
        effectiveHeight < 760f -> TourLayoutMetrics(
            cardMaxWidth = (232f * widthFactor).toInt(),
            headingTopPadding = 10,
            headingFontSize = 20f * widthFactor.coerceAtLeast(0.92f),
            headingLineHeight = 24f * widthFactor.coerceAtLeast(0.92f),
            progressBottomPadding = 16
        )

        effectiveHeight < 860f -> TourLayoutMetrics(
            cardMaxWidth = (260f * widthFactor).toInt(),
            headingTopPadding = 14,
            headingFontSize = 24f * widthFactor,
            headingLineHeight = 29f * widthFactor,
            progressBottomPadding = 20
        )

        else -> TourLayoutMetrics(
            cardMaxWidth = (320f * widthFactor).toInt(),
            headingTopPadding = 24,
            headingFontSize = 32f * widthFactor,
            headingLineHeight = 46f * widthFactor,
            progressBottomPadding = 24
        )
    }
}

private fun legalCopy() = buildAnnotatedString {
    append("By continuing, you agree to Claro's ")
    pushStyle(SpanStyle(color = Ink, textDecoration = TextDecoration.Underline))
    append("Terms of Service")
    pop()
    append(" and ")
    pushStyle(SpanStyle(color = Ink, textDecoration = TextDecoration.Underline))
    append("Privacy Policy")
    pop()
    append(".")
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomePreview() {
    UPMHackItUpTheme(darkTheme = false, dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom)
                    )
                )
        ) {
            OnboardingScreenScaffold(
                primaryLabel = "Get started",
                onPrimaryClick = {}
            ) {
                WelcomeScreen()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TourPreview() {
    UPMHackItUpTheme(darkTheme = false, dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BackgroundTop, BackgroundMiddle, BackgroundBottom)
                    )
                )
        ) {
            OnboardingScreenScaffold(
                primaryLabel = "Continue",
                onPrimaryClick = {}
            ) {
                TourScreen(
                    index = 0,
                    total = TourSlides.size,
                    onBack = {},
                    onNext = {},
                    onSkip = {}
                )
            }
        }
    }
}
