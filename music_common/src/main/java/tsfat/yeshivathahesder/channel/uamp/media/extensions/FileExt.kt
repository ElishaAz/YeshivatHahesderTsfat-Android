/*
 * Copyright 2019 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tsfat.yeshivathahesder.channel.uamp.media.extensions

import android.content.ContentResolver
import android.net.Uri
import java.io.File

/**
 * This file contains extension methods for the java.io.File class.
 */

/**
 * Returns a Content Uri for the AlbumArtContentProvider
 */
fun File.asAlbumArtContentUri(): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AUTHORITY)
        .appendPath(this.path)
        .build()
}

private const val AUTHORITY = "tsfat.yeshivathahesder.channel.uamp"
