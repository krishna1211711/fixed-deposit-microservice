import fitz
import os
import glob

def extract_text_from_pdfs():
    # Find all pdfs in the current directory
    pdf_files = glob.glob("*.pdf")
    
    if not pdf_files:
        print("No PDF files found in the current directory.")
        return

    for pdf_file in pdf_files:
        print(f"Extracting text from: {pdf_file}")
        try:
            # Open the PDF
            doc = fitz.open(pdf_file)
            extracted_text = []
            
            # Iterate through each page
            for page_num in range(len(doc)):
                page = doc.load_page(page_num)
                text = page.get_text()
                if text:
                    extracted_text.append(text)
                
            # Create a corresponding txt file
            base_name = os.path.splitext(pdf_file)[0]
            txt_filename = f"{base_name}.txt"
            
            with open(txt_filename, "w", encoding="utf-8") as f:
                f.write("\n".join(extracted_text))
                
            print(f"Successfully saved text to {txt_filename}")
        except Exception as e:
            print(f"Error extracting {pdf_file}: {e}")

if __name__ == "__main__":
    extract_text_from_pdfs()
