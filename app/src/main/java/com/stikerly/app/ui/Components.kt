package com.stikerly.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/** Tombol bergaya stiker: outline hitam tebal + bayangan solid. */
@Composable
fun ChunkyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Brand.Green,
    contentColor: Color = Color.White,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(Brand.Ink, shape)
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(container, shape)
                .border(2.dp, Brand.Ink, shape)
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = contentColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

/** Kartu dengan outline tebal dan bayangan solid. */
@Composable
fun ChunkyCard(
    modifier: Modifier = Modifier,
    container: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(Brand.Ink, shape)
        )
        val inner = Modifier
            .fillMaxSize()
            .background(container, shape)
            .border(2.dp, Brand.Ink, shape)
            .clip(shape)
        Box(
            if (onClick != null) inner.clickable(onClick = onClick) else inner,
            content = content,
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .background(if (selected) Brand.Green else Color.White, shape)
            .border(2.dp, Brand.Ink, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Brand.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .size(42.dp)
                    .background(Color.White, shape)
                    .border(2.dp, Brand.Ink, shape)
                    .clip(shape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Brand.Ink)
            }
        }
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Brand.Ink)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Brand.GreenDark)
}

@Composable
fun Checkerboard(modifier: Modifier = Modifier, cell: Dp = 12.dp) {
    Canvas(modifier.clipToBounds()) {
        val c = cell.toPx()
        val cols = ceil(size.width / c).toInt()
        val rows = ceil(size.height / c).toInt()
        for (r in 0 until rows) {
            for (col in 0 until cols) {
                drawRect(
                    color = if ((r + col) % 2 == 0) Color(0xFFF2F7F4) else Color(0xFFE1EAE4),
                    topLeft = Offset(col * c, r * c),
                    size = Size(c, c),
                )
            }
        }
    }
}

/** Tulisan "Stickerly" dengan outline hitam seperti di banner. */
@Composable
fun Wordmark(size: Int = 34) {
    val fill = buildAnnotatedString {
        withStyle(SpanStyle(color = Color.White)) { append("Sticker") }
        withStyle(SpanStyle(color = Brand.GreenBright)) { append("ly") }
    }
    Box {
        Text(
            "Stickerly",
            style = TextStyle(
                fontSize = size.sp,
                fontWeight = FontWeight.Black,
                color = Brand.Ink,
                drawStyle = Stroke(width = size * 0.5f, join = StrokeJoin.Round),
            ),
        )
        Text(
            fill,
            style = TextStyle(fontSize = size.sp, fontWeight = FontWeight.Black),
        )
    }
}

@Composable
fun HSpace(w: Dp) {
    Spacer(Modifier.width(w))
}
