package tsfat.yeshivathahesder.core.base

import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Base Fragment which is extended by other fragments
 * in the app
 */
open class BaseFragment : Fragment() {

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

    }
}