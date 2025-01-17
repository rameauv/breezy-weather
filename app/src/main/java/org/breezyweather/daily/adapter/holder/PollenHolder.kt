/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.daily.adapter.holder

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import org.breezyweather.common.ui.composables.PollenGrid
import org.breezyweather.daily.adapter.DailyWeatherAdapter
import org.breezyweather.daily.adapter.model.DailyPollen
import org.breezyweather.databinding.ItemWeatherDailyPollenBinding
import org.breezyweather.theme.compose.BreezyWeatherTheme

class PollenHolder(
    private val mBinding: ItemWeatherDailyPollenBinding
) : DailyWeatherAdapter.ViewHolder(
    mBinding.root
) {
    @SuppressLint("SetTextI18n", "RestrictedApi")
    override fun onBindView(model: DailyWeatherAdapter.ViewModel, position: Int) {
        mBinding.composeView.setContent {
            BreezyWeatherTheme(lightTheme = !isSystemInDarkTheme()) {
                PollenGrid(pollen = (model as DailyPollen).pollen)
            }
        }
    }
}