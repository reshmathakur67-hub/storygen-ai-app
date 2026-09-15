package com.example.ui.export

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.export.ExportResult
import com.example.models.GeneratedVideoProject

@Composable
fun ExportVideoDialog(
    project: GeneratedVideoProject,
    isExporting: Boolean,
    exportProgress: Float,
    statusMessage: String,
    exportResult: ExportResult?,
    errorMessage: String?,
    onOpenVideo: (Uri?, java.io.File?) -> Unit,
    onShareVideo: (Uri?, java.io.File?, String) -> Unit,
    onSaveToDownloads: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = {
        if (!isExporting) onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFF00F5D4).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("export_video_dialog"),
            color = Color(0xFF131122),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00F5D4).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (exportResult != null) "Video Exported!" else "Exporting Video",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isExporting) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp).testTag("close_export_dialog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isExporting) {
                    // Export In Progress State
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F1C36)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.size(68.dp),
                            color = Color(0xFF00F5D4),
                            strokeWidth = 5.dp,
                            trackColor = Color(0xFF2E2B4B)
                        )
                        Text(
                            text = "${(exportProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = project.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = statusMessage,
                        color = Color(0xFF00F5D4),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00F5D4),
                        trackColor = Color(0xFF23203C)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Rendering full MP4 frames with H.264 encoding & subtitles...",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (exportResult != null) {
                    // Success State
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F5D4).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00F5D4),
                                    modifier = Modifier.size(46.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Saved to Device Public Storage!",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Available in device Gallery, Photos, and Media Players",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Metadata Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1B1830),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Folder:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Text(exportResult.destinationFolder, color = Color(0xFF00F5D4), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Resolution:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Text("${exportResult.width} x ${exportResult.height} (${project.aspectRatio.key})", color = Color.White, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration & Size:", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Text("${exportResult.durationSeconds}s • ${exportResult.fileSizeFormatted}", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Action: Open in Player
                            Button(
                                onClick = { onOpenVideo(exportResult.publicUri, exportResult.file) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_video_player_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00F5D4),
                                    contentColor = Color(0xFF0C0A1A)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Play in Video Player", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action: Share Video
                            OutlinedButton(
                                onClick = { onShareVideo(exportResult.publicUri, exportResult.file, project.title) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("share_video_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFFFFD166))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Video", fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Copy to Downloads option
                            OutlinedButton(
                                onClick = onSaveToDownloads,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("save_to_downloads_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White.copy(alpha = 0.85f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = Color(0xFF7209B7))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Also Save Copy to Downloads", fontSize = 13.sp)
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    // Error state
                    Text(
                        text = "Export Notice",
                        color = Color(0xFFFF5964),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23203C))
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
