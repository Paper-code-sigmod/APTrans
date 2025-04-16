# 类定义 定义操作类、图
class OperationGroup:
    def __init__(self, op_id, sql, time, txn_id, operation_type, ids, vals):
        self.id = op_id
        self.time = time
        self.sql = sql
        self.txn_id = str(txn_id)
        self.operation_type = operation_type
        self.ids = ids
        self.vals = vals

    def __repr__(self):
        return (f"OperationGroup(time={self.time}, txn_id={self.txn_id}, "
                f"operation_type={self.operation_type}, old_values={self.ids}, "
                f"new_values={self.vals})")

class DirectedGraph:
    def __init__(self):
        self.graph = {}
        # Edge types counter to keep track of the number of each type
        self.edge_type = {"rw": 0, "ww": 0, "wr": 0}

    def add_edge(self, u, v, e):
        """Add an edge from u to v with the specified edge type."""
        if u not in self.graph:
            self.graph[u] = []
        self.graph[u].append(v)
        
        # Increment the count of the specified edge type
        if e in self.edge_type:
            self.edge_type[e] += 1
        else:
            print(f"Warning: Unknown edge type '{e}'.")

    def display(self, print_middle_result=True):
        if not self.graph:
            if print_middle_result:
                print("Graph is empty")
            return
        if print_middle_result:
            for node, edges in self.graph.items():
                print(f"{node} -> {edges}")
            print(self.edge_type)
    
    def _dfs(self, node, visited, rec_stack):
        visited.add(node)
        rec_stack.add(node)

        # 遍历邻接节点
        for neighbor in self.graph.get(node, []):
            if neighbor not in visited:
                if self._dfs(neighbor, visited, rec_stack):
                    return True
            elif neighbor in rec_stack:
                # 如果邻接节点在递归栈中，则存在环
                return True

        rec_stack.remove(node)
        return False

    def has_cycle(self):
        visited = set()
        rec_stack = set()

        for node in self.graph:
            if node not in visited:
                if self._dfs(node, visited, rec_stack):
                    return True
        return False
    
def ww_dependency(operation1, operation2):
    id1 = operation1.ids
    id2 = operation2.ids
    # 对于写写依赖，只要处理了相同的行，就存在依赖
    if set(id1) & set(id2):
        return True
    else:
        return False

def wr_dependency(operation1, operation2, executor, print_middle_result=True):
    id1 = operation1.ids
    id2 = operation2.ids
    val1 = operation1.vals
    val2 = operation2.vals
    
    # 对于插入操作，读到了新的行则出现问题
    # 对于删除操作，理应读到被删除的行，如果没读到，说明删除影响了读操作，则存在wr依赖
    # 对于更新操作，读到的值发生了改变，我们认为出现了wr依赖
    if operation1.operation_type == "INSERT":
        for i in range(len(id1)):
            if id1[i] in id2:
                return True
    elif operation1.operation_type == "DELETE":
        ids = executor.check_delete_dependency(operation2.sql, print_middle_result)
        ids = set(ids) & set(id1)
        if print_middle_result:
            print(f"ids: {ids}")
        for idx in ids:
            if idx not in id2:
                return True
    elif operation1.operation_type == "UPDATE":
        for i in range(len(id1)):
            if id1[i] in id2 and val1[i] == val2[id2.index(id1[i])]:
                return True
    return False


def rw_dependency(operation1, operation2):
    id1 = operation1.ids
    id2 = operation2.ids
    val1 = operation1.vals
    val2 = operation2.vals
    
    #对于item的读写依赖，只要判断是否处理了相同的行即可
    if set(id1) & set(id2):
        return True
    else:
        return False