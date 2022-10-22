public class LinkDAO {

	public final long id;
	public String linkId;
	public String geometry;

	public LinkDAO(long id, String linkId, String geometry) {
		this.id = id;
		this.linkId = linkId;
		this.geometry = geometry;
	}
}
