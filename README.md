# Graph-core

## Introduction
This project is the core library to projectile an optimized and traversable mega-graph of nodes.


## Main Requirement Vision
1. The graph should easily hold 500,000 nodes and 10,000,000 edges -> done
   * Now can easily hold 1E6 nodes and 1E8 edges under 2G
2. The traversal of the graph nodes should take place in a linear fashion relative to the number of the edges traversed
3. This tool should provide the facility to run graph queries and filters
4. This project must be responsible of persisting the graph changes through writers given