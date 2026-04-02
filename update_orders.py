import sys

file_path = r"c:\Users\Shubham Pathak\OneDrive\Documents\Project\Shopping_Cart\src\main\java\com\ecommerce\service\impl\OrderServiceImpl.java"

with open(file_path, 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if "public ProductOrder updateOrderStatus(Integer id, String status) {" in line and "@Override" in lines[i-1]:
        # We found the method
        # We'll replace it and the next methods until the end of this method
        new_lines.append("\t@Override\n")
        new_lines.append("\t@Transactional\n")
        new_lines.append("\tpublic ProductOrder updateOrderStatus(Integer id, String status) {\n")
        new_lines.append("\t\tOptional<ProductOrder> findById = orderRepository.findById(id);\n")
        new_lines.append("\t\tif (findById.isPresent()) {\n")
        new_lines.append("\t\t\tProductOrder productOrder = findById.get();\n")
        new_lines.append("\t\t\tString oldStatus = productOrder.getStatus();\n")
        new_lines.append("\t\t\tproductOrder.setStatus(status);\n")
        new_lines.append("\t\t\tProductOrder updateOrder = orderRepository.save(productOrder);\n")
        new_lines.append("\n")
        new_lines.append("\t\t\t// If the order is cancelled, return the stock to the product\n")
        new_lines.append("\t\t\tif (OrderStatus.CANCEL.getName().equalsIgnoreCase(status) && !OrderStatus.CANCEL.getName().equalsIgnoreCase(oldStatus)) {\n")
        new_lines.append("\t\t\t\treturnStock(updateOrder.getProduct(), updateOrder.getQuantity());\n")
        new_lines.append("\t\t\t}\n")
        new_lines.append("\n")
        new_lines.append("\t\t\treturn updateOrder;\n")
        new_lines.append("\t\t}\n")
        new_lines.append("\t\treturn null;\n")
        new_lines.append("\t}\n")
        new_lines.append("\n")
        new_lines.append("\tprivate void returnStock(Product product, int quantity) {\n")
        new_lines.append("\t\tif (product == null || product.getId() == null) {\n")
        new_lines.append("\t\t\treturn;\n")
        new_lines.append("\t\t}\n")
        new_lines.append("\t\tProduct latestProduct = productRepository.findById(product.getId()).orElse(null);\n")
        new_lines.append("\t\tif (latestProduct != null) {\n")
        new_lines.append("\t\t\tlatestProduct.setStock(latestProduct.getStock() + quantity);\n")
        new_lines.append("\t\t\tproductRepository.save(latestProduct);\n")
        new_lines.append("\t\t}\n")
        new_lines.append("\t}\n")
        
        # Now find the end of the original method
        j = i
        brace_count = 1
        while brace_count > 0 and j < len(lines):
            j += 1
            if "{" in lines[j]: brace_count += lines[j].count("{")
            if "}" in lines[j]: brace_count -= lines[j].count("}")
        
        # We want to skip from i-1 to j
        # But we already added the new method.
        # So we'll skip the original lines in the main loop.
        skip_range = range(i-1, j+1)
        skip = True
        current_skip_end = j
    
    if not skip:
        new_lines.append(line)
    else:
        if i >= current_skip_end:
            skip = False

with open(file_path, 'w') as f:
    f.writelines(new_lines)
