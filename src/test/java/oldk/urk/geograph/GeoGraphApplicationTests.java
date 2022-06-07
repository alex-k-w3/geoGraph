package oldk.urk.geograph;

import com.slimjars.dist.gnu.trove.list.TLongList;
import com.slimjars.dist.gnu.trove.set.TLongSet;
import com.slimjars.dist.gnu.trove.set.hash.TLongHashSet;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.geometry.GeometryBuilder;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import oldk.urk.geograph.osm.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

//@SpringBootTest
class GeoGraphApplicationTests {

	public static final String FILE_NAME = "/ukraine-latest.osm.pbf";
	//public static final String FILE_NAME = "/kherson_oblast-latest.osm.pbf";

//0 = "district"
//1 = "municipality"
//2 = "borough"
//3 = "state"
// 0 = {Pair@2925} "admin_centre->Node"
//1 = {Pair@2926} "label->Node"
//2 = {Pair@2927} "subarea->Relation"
//3 = {Pair@2928} "outer->Way"
//4 = {Pair@2929} "inner->Way"

//name, name:en, name:uk, name:ru,
// boundary = administrative
}
