package rs.etf.sab.student.utils;

import java.text.SimpleDateFormat;
import java.util.*;

public class UtilityOperations {
	
	public static int getCityByName(String name) {
		Logger.functionStart("UtilityOperations getCityByName(name: " + name + ")");
		
		Result city = DB.select("City", new Where("Name", "=", name));
		
		int result = city.isEmpty() ? -1 : (int) city.get("CityID");
		
		Logger.functionEnd(result);
		return result;
	}
	
	public static boolean getDiscountEligability(int orderId, String date) {
		Logger.functionStart("UtilityOperations getDiscountEligability(orderId: " + orderId + ", date: " + date + ")");
		
		boolean result = (int) DB.procedure("spMoreDiscount", String.valueOf(orderId), date) == 1;
		
		Logger.functionEnd(result);
		return result;
	}
	
	public static String getDateFromCalendar(Calendar calendar) {
		return getDateFromCalendar(calendar, 0);
	}
	
	public static String getDateFromCalendar(Calendar calendar, int days) {
		Logger.functionStart("UtilityOperations getDateFromCalendar(calendar: " + calendar + ", days: " + days + ")");
		
		Date result = calendar.getTime();
		result.setTime(result.getTime() + (long) days * 24 * 60 * 60 * 1000);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		String resultString = dateFormat.format(result);
		
		Logger.functionEnd(resultString);
		return resultString;
	}
	
	
	
	private static class Node {
		int id;
		boolean endNode;
		boolean visited;
		Map<Node, Integer> neighbors;
		
		public Node(int id) {
			this.id = id;
			this.neighbors = new HashMap<>();
			this.endNode = false;
			this.visited = false;
		}
		
		public void setEndNode(boolean endNode) {
			this.endNode = endNode;
		}
		
		public void addNeighbor(Node neighbor, int weight) {
			neighbors.put(neighbor, weight);
		}
	}
	
	private static HashMap<Integer, Node> buildGraph() {
		Result cities = DB.select("City");
		Result connections = DB.select("Connection");
		
		HashMap<Integer, Node> graph = new HashMap<>();
		
		for (Entry city : cities) {
			graph.put((int) city.get("CityID"), new Node((int) city.get("CityID")));
		}
		
		for (Entry connection : connections) {
			int cityId1 = (int) connection.get("CityID1");
			int cityId2 = (int) connection.get("CityID2");
			int distance = (int) connection.get("Distance");
			
			graph.get(cityId1).addNeighbor(graph.get(cityId2), distance);
			graph.get(cityId2).addNeighbor(graph.get(cityId1), distance);
		}
		
		return graph;
	}
	
	public static ArrayList<Integer> shortestPath(int startId, int destinationId) {
		Logger.functionStart("UtilityOperations shortestPath(startId: " + startId + ", destinationId: " + destinationId + ")");
		
		Map<Integer, Node> cityMap = buildGraph();
		
		Node start = cityMap.get(startId);
		Node destination = cityMap.get(destinationId);
		
		HashMap<Node, Integer> distances = new HashMap<>();
		HashMap<Node, Node> previous = new HashMap<>();
		PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));
		
		queue.add(start);
		distances.put(start, 0);
		
		while (!queue.isEmpty()) {
			Node current = queue.poll();
			
			if (current == destination) {
				break;
			}
			
			current.visited = true;
			
			for (Map.Entry<Node, Integer> neighbor : current.neighbors.entrySet()) {
				Node neighborNode = neighbor.getKey();
				int distance = neighbor.getValue();
				
				if (!neighborNode.visited) {
					int newDistance = distances.get(current) + distance;
					
					if (!distances.containsKey(neighborNode) || newDistance < distances.get(neighborNode)) {
						distances.put(neighborNode, newDistance);
						previous.put(neighborNode, current);
						queue.add(neighborNode);
					}
				}
			}
		}
		
		ArrayList<Integer> path = new ArrayList<>();
		
		Node current = destination;
		
		while (current != null) {
			path.add(current.id);
			current = previous.get(current);
		}
		
		Collections.reverse(path);
		
		Logger.functionEnd(path);
		return path;
	}
	
	public static int findClosestStoreCity(int destinationId) {
		Logger.functionStart("UtilityOperations findClosestStoreCity(destinationId: " + destinationId + ")");
		
		Map<Integer, Node> cityMap = buildGraph();
		
		for (Node city : cityMap.values()) {
			Result stores = DB.select("Shop", new Where("CityID", "=", city.id));
			city.setEndNode(!stores.isEmpty());
		}
		
		Node destination = cityMap.get(destinationId);
		
		HashMap<Node, Integer> distances = new HashMap<>();
		PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(distances::get));
		
		queue.add(destination);
		distances.put(destination, 0);
		
		while (!queue.isEmpty()) {
			Node current = queue.poll();
			
			if (current.endNode) {
				Logger.functionEnd(current.id);
				return current.id;
			}
			
			current.visited = true;
			
			for (Map.Entry<Node, Integer> neighbor : current.neighbors.entrySet()) {
				Node neighborNode = neighbor.getKey();
				int distance = neighbor.getValue();
				
				if (!neighborNode.visited) {
					int newDistance = distances.get(current) + distance;
					
					if (!distances.containsKey(neighborNode) || newDistance < distances.get(neighborNode)) {
						distances.put(neighborNode, newDistance);
						queue.add(neighborNode);
					}
				}
			}
		}
		
		Logger.functionEnd(-1);
		return -1;
	}
}
