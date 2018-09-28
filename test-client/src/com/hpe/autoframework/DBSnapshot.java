package com.hpe.autoframework;

import org.dbunit.dataset.ITable;

public class DBSnapshot {
	
	private int diffResultIndex_ = 1;
	
	private DBClient dbClient_;
	private String query_;
	private String[] keys_;
	private String resultname_;
	private ITable table_;
	
	public DBSnapshot(DBClient dbClient, String query, String resultname, ITable table) {
		this(dbClient, query, new String[0], resultname, table);
	}
	
	public DBSnapshot(DBClient dbClient, String query, String[] keys, String resultname, ITable table) {
		dbClient_ = dbClient;
		query_ = query;
		keys_ = keys;
		resultname_ = resultname;
		table_ = table;
	}
	
	public ITable getTable() {
		return table_;
	}
	
	public void diff() {
		table_ = dbClient_.diff(table_, getResultName(), query_, keys_);
	}
	
	public void diff(boolean diffOnly) {
		table_ = dbClient_.diff(table_, getResultName(), query_, keys_, diffOnly);
	}
	
	private String getResultName() {
		return resultname_ + String.format("_%05d", diffResultIndex_ ++);
	}
}
