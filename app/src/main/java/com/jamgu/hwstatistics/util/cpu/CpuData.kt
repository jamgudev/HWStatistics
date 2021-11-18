/*
 * Copyright 2020 Alexandros Schillings
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jamgu.hwstatistics.util.cpu

import java.util.Collections

data class CpuData constructor(val statFile: String, val overallCpu: Float, private val perCpuUtilisation: List<Float>, val hasError: Boolean) {

    constructor(statFile: String, overallCpu: Float, perCpuUtilisation: List<Float>) : this(statFile, overallCpu, perCpuUtilisation, false)
    constructor(statFile: String, error: Boolean) : this(statFile, 0f, emptyList<Float>(), error)

    fun getPerCpuUtilisation(): List<Float> {
        return Collections.unmodifiableList(perCpuUtilisation)
    }
}