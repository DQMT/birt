/*******************************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation  - initial API and implementation
 *******************************************************************************/
package org.eclipse.birt.data.engine.executor.cache.disk;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import org.eclipse.birt.data.engine.core.DataException;
import org.eclipse.birt.data.engine.odi.IResultClass;
import org.eclipse.birt.data.engine.odi.IResultObject;
import org.eclipse.birt.data.engine.executor.cache.ResultObjectUtil;
import org.eclipse.birt.data.engine.executor.cache.IRowResultSet;

/**
 * When available memory can not accomodate existing data, it will rely on this
 * class to do data sorting and data soring to file. This class also provides
 * convinienent method to retrieve data from file.
 */
class DiskCacheResultSet
{
	private Map infoMap;
	private int dataCount;
	
	private DiskDataExport databaseExport;	
	private ResultObjectUtil resultObjectUtil;
	
	private IRowIterator rowIterator;
	
	/**
	 * @param dataProvider
	 */
	DiskCacheResultSet( Map infoMap )
	{
		this.infoMap = infoMap;
	}

	/**
	 * @param resultObjects
	 * @param comparator
	 * @throws IOException, file writer exception
	 */
	public void processStartResultObjects( IResultObject[] resultObjects,
			Comparator comparator ) throws IOException
	{
		IResultClass rsMetaData = resultObjects[0].getResultClass( );
		assert rsMetaData != null;
		this.resultObjectUtil = ResultObjectUtil.newInstance( rsMetaData );
		
		databaseExport = DiskDataExport.newInstance( infoMap,
				comparator,
				rsMetaData,
				resultObjectUtil );		
		databaseExport.exportStartDataToDisk( resultObjects );
		dataCount = resultObjects.length;
	}
	
	/**
	 * @param resultObject, the start resultObject
	 * @param rs, follows the resultObject
	 * @throws DataException
	 * @throws IOException
	 */
	public void processRestResultObjects( IResultObject resultObject,
			IRowResultSet rs ) throws DataException, IOException
	{
		dataCount += databaseExport.exportRestDataToDisk( resultObject, rs );
		rowIterator = databaseExport.getRowIterator( );
	}
	
	/**
	 * @return the length of result set
	 */
	public int getCount( )
	{
		return dataCount;
	}
	
	/**
	 * This function must be called after goal file is generated.
	 * 
	 * @return RowData
	 * @throws IOException, file reader exception
	 */
	public IResultObject nextRow( ) throws IOException
	{
		return rowIterator.fetch( );
	}
	
	/**
	 * Set the file reader to the start of the goal file
	 */
	public void reset( )
	{
		rowIterator.reset( );
	}
	
	/**
	 * Close result set
	 */
	public void close( )
	{
		if ( rowIterator != null )
		{
			rowIterator.close( );
			rowIterator = null;
		}
		if ( databaseExport != null )
		{
			databaseExport.close( );
			databaseExport = null;
		}
		resultObjectUtil = null;
	}
	
}
