/******************************************************************************
 *project: org.matsim.*
 * Blocks.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.graph.block;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.graph.block.xmlblocks.BlockBorder;
import playground.rost.graph.block.xmlblocks.BlockList;
import playground.rost.graph.block.xmlblocks.SingleBlock;

public class Blocks {
	protected Collection<Block> blocks;
	
	public Blocks()
	{
		this.blocks = new HashSet<Block>();
	}
	
	public Blocks(Collection<Block> blocks)
	{
		this.blocks = blocks;
	}
	
	public void addBlock(Block b)
	{
		blocks.add(b);
	}
	
	public Collection<Block> getBlocks()
	{
		return blocks;
	}
	
	public static Blocks readXMLFile(NetworkLayer network, String filename) throws JAXBException, IOException
	{
		Blocks blocks = new Blocks();
		
		JAXBContext context = JAXBContext.newInstance("playground.rost.graph.block.xmlblocks");

		Unmarshaller unmarshaller = context.createUnmarshaller();
		
		BlockList bList = (BlockList)unmarshaller.unmarshal(new FileReader(filename));
		
		for(SingleBlock block : bList.getBlock())
		{
			Block b = Block.create(network, block.getId(), block.getX(), block.getY(), block.getAreaSize(), block.getBorder().getNodes());
			blocks.addBlock(b);
		}
		
		return blocks;
	}
	
	public void writeXMLFile(String filename) throws JAXBException, IOException
	{
		BlockList blockList = new BlockList();
		
		for(Block b : blocks)
		{
			SingleBlock singleBlock = createSingleBlock(b);
			blockList.getBlock().add(singleBlock);
		}
		JAXBContext jaxbContext = JAXBContext.newInstance("playground.rost.graph.block.xmlblocks");
		Marshaller marshall = jaxbContext.createMarshaller();
		marshall.marshal(blockList, new FileWriter(filename));
	}
	
	protected SingleBlock createSingleBlock(Block b)
	{
		SingleBlock block = new SingleBlock();
		block.setId(b.id);
		block.setAreaSize(b.area_size);
		block.setX(b.x_mean);
		block.setY(b.y_mean);
		BlockBorder bBorder = new BlockBorder();
		for(Node node : b.border)
		{
			if(node == null)
				throw new RuntimeException("invalid border while writing xmlblock");
			String id = node.getId().toString();
			bBorder.getNodes().add(id);
		}
		block.setBorder(bBorder);
		return block;
	}

	
}
