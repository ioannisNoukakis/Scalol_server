import com.google.inject.{Inject, Singleton}
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter

@Singleton
class Filters @Inject()(
                         corsFilter: CORSFilter) extends HttpFilters {

  override def filters: Seq[EssentialFilter] = Seq(
    corsFilter
  )
}