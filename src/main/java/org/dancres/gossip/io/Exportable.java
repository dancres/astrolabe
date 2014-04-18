package org.dancres.gossip.io;

import java.io.IOException;
import java.io.Writer;

/**
 * @todo Maybe add an import method too...
 */
public interface Exportable {
	public void export(Writer aWriter) throws IOException;
}
