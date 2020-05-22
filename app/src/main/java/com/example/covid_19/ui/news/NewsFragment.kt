package com.example.covid_19.ui.news

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.covid_19.Headline
import com.example.covid_19.R
import kotlinx.android.synthetic.main.fragment_news.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.lang.ref.WeakReference
import java.net.URL
import java.nio.charset.Charset
import javax.net.ssl.HttpsURLConnection

class NewsFragment : Fragment() {

    private lateinit var newsViewModel: NewsViewModel
    private var KEY = "a10bc7fd2caf45058eef8547fb8e7b74"
    private lateinit var sourcesPicker: Spinner
    private lateinit var newsPicker : RecyclerView

    var headlines: List<Headline> = listOf()
        set(value) {
            field = value
            newsPicker.adapter = NewsAdapter(this.requireContext(), field) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.url))
                startActivity(intent)
            }
        }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        newsViewModel =
                ViewModelProviders.of(this).get(NewsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_news, container, false)

        val parameters = mapOf("q" to "covid", "apiKey" to KEY, "country" to "nz")
        val url = parameterizeUrl("https://newsapi.org/v2/top-headlines", parameters)
        HeadlinesDownloader(this).execute(url)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsPicker = view!!.findViewById<RecyclerView>(R.id.headlinesPicker)
        val layoutManager = LinearLayoutManager(this.requireContext())
        newsPicker.layoutManager = layoutManager
//        val decoration = DividerItemDecoration(this.requireContext(), layoutManager.orientation)
//        newsPicker.addItemDecoration(decoration)

    }


    @SuppressLint("StaticFieldLeak")
    inner class HeadlinesDownloader(val activity: NewsFragment) : AsyncTask<URL, Void, List<Headline>>() {
        private val context = WeakReference(activity)

        override fun doInBackground(vararg urls: URL): List<Headline> {
            val result = getJson(urls[0])

            val headlinesJson = result.getJSONArray("articles")
            val headlines = (0 until headlinesJson.length()).map { i ->
                val headline = headlinesJson.getJSONObject(i)
                Headline(
                    headline.getString("title"),
                    headline.getString("urlToImage"),
                    headline.getString("publishedAt"),
                    headline.getString("url")
                )
            }
            return headlines
        }

        override fun onPostExecute(headlines: List<Headline>) {
            super.onPostExecute(headlines)
            context.get()?.headlines = headlines
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun parameterizeUrl(url: String, parameters: Map<String, String>): URL {
        val builder = Uri.parse(url).buildUpon()
        parameters.forEach { key, value -> builder.appendQueryParameter(key, value) }
        val uri = builder.build()
        return URL(uri.toString())
    }

    private fun getJson(url: URL): JSONObject {
        val connection = url.openConnection() as HttpsURLConnection
        try {
            val json = BufferedInputStream(connection.inputStream).readBytes().toString(Charset.defaultCharset())
            return JSONObject(json)
        } finally {
            connection.disconnect()
        }
    }


}
